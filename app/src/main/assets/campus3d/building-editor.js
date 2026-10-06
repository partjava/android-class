
  function showExportModal(title, jsonStr){
   if(window.CampusNativeBridge && typeof window.CampusNativeBridge.copyToClipboard === 'function'){
    window.CampusNativeBridge.copyToClipboard(jsonStr);
   }
   let modal = document.getElementById('export-modal');
   if(!modal){
    modal = document.createElement('div');
    modal.id = 'export-modal';
    modal.style.cssText = 'position:fixed;inset:0;background:rgba(0,0,0,0.65);backdrop-filter:blur(5px);z-index:99999;display:flex;align-items:center;justify-content:center;padding:16px;pointer-events:auto;';
    modal.innerHTML = `
     <div style="background:white;border-radius:14px;max-width:92%;width:420px;max-height:85vh;display:flex;flex-direction:column;box-shadow:0 12px 32px rgba(0,0,0,0.3);overflow:hidden;">
       <div style="display:flex;align-items:center;justify-content:between;padding:12px 16px;background:#f5f7f6;border-bottom:1px solid #e1e7e4;">
         <span id="export-modal-title" style="font-weight:bold;font-size:14px;color:#1e4d3c;flex:1;">📋 数据导出</span>
         <button id="export-modal-close" style="background:#eee;border:0;border-radius:50%;width:26px;height:26px;cursor:pointer;font-weight:bold;color:#666;">✕</button>
       </div>
       <div style="padding:14px 16px;display:flex;flex-direction:column;gap:10px;flex:1;overflow:hidden;">
         <span style="font-size:12px;color:#456657;line-height:1.4;">✅ 已为您自动复制到剪贴板！<br>若复制未成功，您也可长按下方文本框全选复制：</span>
         <textarea id="export-modal-area" style="width:100%;height:200px;box-sizing:border-box;font-family:monospace;font-size:11px;padding:8px;border:1px solid #ccd6d0;border-radius:8px;resize:none;background:#fafafa;" readonly></textarea>
         <button id="export-modal-copy" style="padding:10px;background:#215e48;color:white;font-weight:bold;border:0;border-radius:8px;cursor:pointer;font-size:13px;">📋 点击再次复制到剪贴板</button>
       </div>
     </div>
    `;
    document.body.appendChild(modal);
    document.getElementById('export-modal-close').onclick = () => { modal.style.display = 'none'; };
    document.getElementById('export-modal-copy').onclick = () => {
     const ta = document.getElementById('export-modal-area');
     ta.select();
     if(window.CampusNativeBridge && typeof window.CampusNativeBridge.copyToClipboard === 'function'){
      window.CampusNativeBridge.copyToClipboard(ta.value);
     }else{
      document.execCommand('copy');
      alert('已复制到剪贴板！');
     }
    };
   }
   document.getElementById('export-modal-title').textContent = title;
   const ta = document.getElementById('export-modal-area');
   ta.value = jsonStr;
   modal.style.display = 'flex';
   ta.select();
  }
  window.showExportModal = showExportModal;
/**
 * 晴川 3D 校园 · 建筑与场地全要素位移与旋转微调器 (Building & Venue Transform Editor)
 * 支持：
 * 1. 全要素选取：支持所有建筑、驾校、田径足球场、篮球场、羽毛球场、排球场、小吃街、情缘湖等
 * 2. 上下左右平移：支持 0.5m / 1m / 3m 步长精细对齐底图
 * 3. 角度旋转：支持 1° / 5° / 15° 顺逆时针绕中心旋转
 * 4. 实时高亮反馈与标签同步
 * 5. 本地持久化保存 (localStorage) 与 一键代码导出
 */
(function(root){
 function createBuildingEditor(){
  let isEditing=false;
  let isCollapsed=false;
  let selectedId=null;
  let stepMove=1.0; // 米
  let stepRot=5;    // 度 (DEG)
  let transforms={}; // { [id]: { dx: 0, dz: 0, rot: 0 } }
  let baseTransforms={}; // { [id]: { x, z, rot, cx, cz, isRegion } }

  let sceneRef=null, cameraRef=null, rendererRef=null;
  let groupsRef=null, labelsRef=null, placesRef=[], selectFn=null;
  let highlightMarker=null;

  function init({scene, camera, renderer, groups, labels, CampusData, select, allPlaces}){
   sceneRef=scene; cameraRef=camera; rendererRef=renderer;
   groupsRef=groups; labelsRef=labels; selectFn=select;
   placesRef=allPlaces || (CampusData ? [...CampusData.buildings, ...(CampusData.regions||[])] : []);

   // 记录所有建筑与运动场地的初始基准位置与旋转
   for(const b of placesRef){
    const g=groupsRef.get(b.id);
    if(g){
     baseTransforms[b.id]={
      x: g.position.x,
      z: g.position.z,
      rot: g.rotation.y,
      cx: b.x || 0,
      cz: b.z || 0,
      isRegion: Boolean(b.region)
     };
    }
   }

   // 选中光圈指示器
   const ringGeo=new THREE.RingGeometry(2, 2.8, 32);
   ringGeo.rotateX(-Math.PI/2);
   const ringMat=new THREE.MeshBasicMaterial({color:'#ffc83b', side:THREE.DoubleSide, transparent:true, opacity:.85});
   highlightMarker=new THREE.Mesh(ringGeo, ringMat);
   highlightMarker.position.y=0.45;
   highlightMarker.visible=false;
   scene.add(highlightMarker);

   loadTransforms();
   createUI();
  }

  function loadTransforms(){
   if(window.CampusNativeBridge && typeof window.CampusNativeBridge.loadConfig === 'function'){
    try{
     const nativeStr = window.CampusNativeBridge.loadConfig('custom_building_transforms');
     if(nativeStr){ transforms = JSON.parse(nativeStr); return; }
    }catch(e){}
   }
   try{
    if(typeof localStorage !== 'undefined' && localStorage){
     const stored = localStorage.getItem('custom_building_transforms');
     if(stored) transforms = JSON.parse(stored);
    }
   }catch(e){}
  }

  function applyTransform(id){
   const g=groupsRef.get(id);
   const b=placesRef.find(item=>item.id===id);
   const base=baseTransforms[id];
   if(!g||!b||!base)return;

   const t=transforms[id]||{dx:0, dz:0, rot:0};
   const rad=((t.rot||0)*Math.PI/180);

   if(base.isRegion){
    // 场地（驾校、篮球场、足球场、羽毛球场等）：绕自身中心 (cx, cz) 旋转与平移
    const cx=base.cx||0, cz=base.cz||0;
    const cos=Math.cos(rad), sin=Math.sin(rad);
    g.rotation.y=base.rot+rad;
    g.position.x=base.x+(t.dx||0)+cx-(cx*cos+cz*sin);
    g.position.z=base.z+(t.dz||0)+cz-(-cx*sin+cz*cos);

    b.x=base.cx+(t.dx||0);
    b.z=base.cz+(t.dz||0);
   }else{
    // 普通建筑模型：Group 原点即为自身中心 (b.x, b.z)
    g.position.x=base.x+(t.dx||0);
    g.position.z=base.z+(t.dz||0);
    g.rotation.y=base.rot+rad;

    b.x=g.position.x;
    b.z=g.position.z;
   }

   if(labelsRef){
    const lbl=labelsRef.find(item=>item.b.id===id);
    if(lbl&&lbl.s&&lbl.s.position){
     lbl.s.position.x=b.x;
     lbl.s.position.z=b.z;
    }
   }

   if(highlightMarker&&selectedId===id){
    highlightMarker.position.x=b.x;
    highlightMarker.position.z=b.z;
   }
  }

  function applyAllTransforms(){
   for(const id of Object.keys(baseTransforms)){
    applyTransform(id);
   }
  }

  function selectBuilding(id){
   if(!id||!baseTransforms[id])return;
   selectedId=id;

   // 恢复所有物体材质原色，高亮选中的物体
   for(const [gId, g] of groupsRef){
    g.traverse(m=>{
     if(m.isMesh&&m.material&&m.material.emissive){
      m.material.emissive.set(gId===id?'#554400':'#000000');
     }
    });
   }

   // 移动光圈到物体底部
   const b=placesRef.find(item=>item.id===id);
   if(b&&highlightMarker){
    highlightMarker.position.x=b.x;
    highlightMarker.position.z=b.z;
    highlightMarker.visible=true;
   }

   // 同步下拉框
   const sel=document.getElementById('bld-select');
   if(sel&&sel.value!==id)sel.value=id;

   updateReadout();
   showToast('已选中：'+(b?b.name:id)+'，可通过下方按钮调整位置与旋转');
  }

  function nudge(dx, dz, drot){
   if(!selectedId)return;
   if(!transforms[selectedId])transforms[selectedId]={dx:0, dz:0, rot:0};
   const t=transforms[selectedId];
   t.dx=Math.round((t.dx+dx)*10)/10;
   t.dz=Math.round((t.dz+dz)*10)/10;
   t.rot=Math.round((t.rot+drot)*10)/10;
   applyTransform(selectedId);
   updateReadout();
  }

  function resetCurrent(){
   if(!selectedId)return;
   transforms[selectedId]={dx:0, dz:0, rot:0};
   applyTransform(selectedId);
   updateReadout();
   showToast('已复位该对象至默认位置');
  }

  function resetAll(){
   if(confirm('确定要将所有建筑和场地恢复至初始默认位置吗？')){
    transforms={};
    applyAllTransforms();
    updateReadout();
    try{localStorage.removeItem('custom_building_transforms');}catch(e){}
    showToast('已全部恢复默认位置');
   }
  }

  function saveTransforms(){
   try{
    localStorage.setItem('custom_building_transforms', JSON.stringify(transforms));
    showToast('🎉 位置与角度已成功保存到本地！');
   }catch(e){
    alert('保存失败：'+e.message);
   }
  }

  function exportTransformsCode(){
   const active={};
   for(const [id, t] of Object.entries(transforms)){
    if(t.dx!==0||t.dz!==0||t.rot!==0){
     active[id]=t;
    }
   }
   const json=JSON.stringify(active, null, 2);
   if(navigator.clipboard&&navigator.clipboard.writeText){
    navigator.clipboard.writeText(json).catch(()=>{});
   }
   prompt('位置与旋转偏移配置 JSON（已复制，可发给我固化到代码）：', json);
  }

  function updateReadout(){
   const info=document.getElementById('bld-readout');
   const t=(selectedId&&transforms[selectedId])?transforms[selectedId]:{dx:0, dz:0, rot:0};
   if(info){
    const b=placesRef.find(item=>item.id===selectedId);
    const name=b?b.name:'未选择';
    info.innerHTML=`<strong>${name}</strong>: X偏: <span style="color:#d46238;">${t.dx>=0?'+':''}${t.dx}m</span>, Z偏: <span style="color:#d46238;">${t.dz>=0?'+':''}${t.dz}m</span>, 角度: <span style="color:#215e48;">${t.rot>=0?'+':''}${t.rot}°</span>`;
   }
  }

  function showToast(msg){
   const el=document.getElementById('bld-toast');
   if(el){
    el.textContent=msg;
    el.style.opacity='1';
    clearTimeout(el._t);
    el._t=setTimeout(()=>{el.style.opacity='0';},2500);
   }
  }

  function createUI(){
   // 1. 在右侧工具栏添加【移楼】按钮
   const tools=document.querySelector('.tools');
   if(tools&&!document.getElementById('building-editor-btn')){
    const btn=document.createElement('button');
    btn.id='building-editor-btn';
    btn.textContent='移楼';
    btn.title='开启建筑与场地位置/旋转微调面板';
    btn.onclick=toggleEditor;
    tools.insertBefore(btn, document.getElementById('top'));
   }

   // 2. 创建建筑变换悬浮操作面板
   const hud=document.createElement('div');
   hud.id='building-editor-hud';
   hud.style.cssText='display:none;position:fixed;inset:0;pointer-events:none;z-index:9;font-family:system-ui,-apple-system,sans-serif;';
   hud.innerHTML=`
    <div id="bld-panel-full" style="position:absolute;top:10px;left:10px;right:10px;background:rgba(255,255,255,0.96);backdrop-filter:blur(10px);padding:10px 12px;border-radius:14px;box-shadow:0 6px 20px rgba(0,0,0,0.2);pointer-events:auto;display:flex;flex-direction:column;gap:8px;">
      <!-- 第一行：标题 + 展开收起 + 退出 -->
      <div style="display:flex;align-items:center;justify-content:space-between;border-bottom:1px solid #e2ebe6;padding-bottom:6px;">
        <div style="display:flex;align-items:center;gap:6px;">
          <span style="font-weight:bold;font-size:13px;color:#1e4d3c;">🏢 物体位置与方向微调</span>
          <select id="bld-select" style="font-size:11px;padding:3px 6px;border-radius:6px;border:1px solid #c2d4cb;background:white;color:#1e4d3c;outline:none;max-width:145px;">
            <option value="">-- 点击物体或下拉选择 --</option>
          </select>
        </div>
        <div style="display:flex;align-items:center;gap:6px;">
          <button id="bld-collapse-btn" style="padding:4px 9px;font-size:11px;border-radius:6px;background:#e2eeea;color:#1d4e3d;border:0;cursor:pointer;font-weight:bold;">▲ 收起面板</button>
          <button id="bld-close-btn" style="padding:4px 9px;font-size:11px;border-radius:6px;background:#444;color:white;border:0;cursor:pointer;">✕ 退出</button>
        </div>
      </div>

      <!-- 第二行：数值状态指示器 -->
      <div id="bld-readout" style="font-size:11px;color:#334d42;background:#f2f7f4;padding:4px 8px;border-radius:6px;">请点击场景中的建筑或场地开始调整</div>

      <!-- 第三行：方向键与旋转控制键盘 (D-Pad) -->
      <div style="display:flex;align-items:center;justify-content:space-around;gap:12px;padding:4px 0;">
        <!-- 平移十字键 -->
        <div style="display:grid;grid-template-columns:repeat(3, 44px);grid-template-rows:repeat(3, 32px);gap:4px;align-items:center;justify-items:center;">
          <div style="grid-column:2;grid-row:1;"><button id="bld-up" style="width:44px;height:32px;padding:0;background:#215e48;color:white;font-weight:bold;font-size:14px;border-radius:6px;border:0;cursor:pointer;" title="北/上移">↑ 北</button></div>
          <div style="grid-column:1;grid-row:2;"><button id="bld-left" style="width:44px;height:32px;padding:0;background:#215e48;color:white;font-weight:bold;font-size:14px;border-radius:6px;border:0;cursor:pointer;" title="西/左移">← 西</button></div>
          <div style="grid-column:3;grid-row:2;"><button id="bld-right" style="width:44px;height:32px;padding:0;background:#215e48;color:white;font-weight:bold;font-size:14px;border-radius:6px;border:0;cursor:pointer;" title="东/右移">东 →</button></div>
          <div style="grid-column:2;grid-row:3;"><button id="bld-down" style="width:44px;height:32px;padding:0;background:#215e48;color:white;font-weight:bold;font-size:14px;border-radius:6px;border:0;cursor:pointer;" title="南/下移">↓ 南</button></div>
        </div>

        <!-- 旋转按钮与步长选择 -->
        <div style="display:flex;flex-direction:column;gap:6px;flex:1;max-width:170px;">
          <div style="display:flex;gap:6px;">
            <button id="bld-rot-ccw" style="flex:1;padding:7px 0;background:#358164;color:white;font-size:12px;font-weight:bold;border-radius:6px;border:0;cursor:pointer;" title="逆时针旋转">↺ 左旋</button>
            <button id="bld-rot-cw" style="flex:1;padding:7px 0;background:#358164;color:white;font-size:12px;font-weight:bold;border-radius:6px;border:0;cursor:pointer;" title="顺时针旋转">↻ 右旋</button>
          </div>
          <div style="display:flex;align-items:center;gap:4px;">
            <span style="font-size:10px;color:#597368;">步长:</span>
            <button id="bld-step-05" style="flex:1;padding:3px 0;font-size:10px;border-radius:4px;background:#e5ece8;color:#284d40;border:0;cursor:pointer;">0.5m</button>
            <button id="bld-step-1" style="flex:1;padding:3px 0;font-size:10px;border-radius:4px;background:#215e48;color:white;border:0;cursor:pointer;">1.0m</button>
            <button id="bld-step-3" style="flex:1;padding:3px 0;font-size:10px;border-radius:4px;background:#e5ece8;color:#284d40;border:0;cursor:pointer;">3.0m</button>
          </div>
          <div style="display:flex;align-items:center;gap:4px;">
            <span style="font-size:10px;color:#597368;">角度:</span>
            <button id="bld-rot-1" style="flex:1;padding:3px 0;font-size:10px;border-radius:4px;background:#e5ece8;color:#284d40;border:0;cursor:pointer;">1°</button>
            <button id="bld-rot-5" style="flex:1;padding:3px 0;font-size:10px;border-radius:4px;background:#215e48;color:white;border:0;cursor:pointer;">5°</button>
            <button id="bld-rot-15" style="flex:1;padding:3px 0;font-size:10px;border-radius:4px;background:#e5ece8;color:#284d40;border:0;cursor:pointer;">15°</button>
          </div>
        </div>
      </div>

      <!-- 第四行：保存、复位与导出 -->
      <div style="display:flex;align-items:center;justify-content:space-between;gap:6px;padding-top:4px;border-top:1px dashed #e2ebe6;">
        <div style="display:flex;gap:5px;">
          <button id="bld-reset-cur" style="padding:5px 8px;font-size:11px;border-radius:6px;background:#dfd8ce;color:#352e25;border:0;cursor:pointer;">↺ 复位当前</button>
          <button id="bld-reset-all" style="padding:5px 8px;font-size:11px;border-radius:6px;background:#ecdada;color:#8f2d2d;border:0;cursor:pointer;">全部复位</button>
          <button id="bld-export" style="padding:5px 8px;font-size:11px;border-radius:6px;background:#e2ece7;color:#1b4a39;border:0;cursor:pointer;">📋 导出代码</button>
        </div>
        <button id="bld-save" style="padding:6px 14px;font-size:12px;font-weight:bold;border-radius:8px;background:#e37036;color:white;border:0;cursor:pointer;box-shadow:0 3px 10px rgba(227,112,54,0.45);">💾 保存生效</button>
      </div>
    </div>

    <!-- 收起状态极简微调胶囊 -->
    <div id="bld-panel-mini" style="display:none;position:absolute;top:10px;left:10px;background:rgba(255,255,255,0.92);backdrop-filter:blur(8px);padding:6px 14px;border-radius:24px;box-shadow:0 4px 14px rgba(0,0,0,0.2);pointer-events:auto;align-items:center;gap:10px;">
      <span style="font-weight:bold;font-size:12px;color:#1e4d3c;">🏢 调整中</span>
      <span id="bld-mini-name" style="font-size:11px;color:#3e6354;">点击任意建筑或场地</span>
      <button id="bld-expand-btn" style="padding:3px 10px;font-size:11px;font-weight:bold;border-radius:12px;background:#215e48;color:white;border:0;cursor:pointer;">▼ 展开面板</button>
    </div>

    <!-- 底部操作提示 -->
    <div id="bld-toast" style="position:absolute;bottom:40px;left:50%;transform:translateX(-50%);background:rgba(20,38,30,0.92);color:white;padding:8px 18px;border-radius:20px;font-size:12px;pointer-events:none;transition:opacity .3s ease;opacity:0;white-space:nowrap;box-shadow:0 4px 14px rgba(0,0,0,0.3);z-index:10;">点击场景建筑或场地进行微调</div>
   `;
   document.body.appendChild(hud);

   // 分类填充全要素下拉框
   const sel=document.getElementById('bld-select');
   if(sel&&placesRef.length>0){
    sel.innerHTML='<option value="">-- 点击物体或下拉选择 --</option>';
    const catMap={
     '教学':'🏫 教学与综合办公',
     '住宿':'🛏️ 宿舍生活公寓',
     '生活':'🏪 生活服务与驾校场地',
     '文体':'⚽ 运动场地与园林景观'
    };
    ['教学','住宿','生活','文体'].forEach(cat=>{
     const list=placesRef.filter(p=>p.category===cat);
     if(list.length>0){
      const grp=document.createElement('optgroup');
      grp.label=catMap[cat]||cat;
      list.forEach(b=>{
       const opt=document.createElement('option');
       opt.value=b.id;
       opt.textContent=b.name;
       grp.appendChild(opt);
      });
      sel.appendChild(grp);
     }
    });

    sel.onchange=()=>{
     if(sel.value)selectBuilding(sel.value);
    };
   }

   // 折叠与展开
   const panelFull=document.getElementById('bld-panel-full');
   const panelMini=document.getElementById('bld-panel-mini');
   const setCollapse=(collapsed)=>{
    isCollapsed=collapsed;
    panelFull.style.display=collapsed?'none':'flex';
    panelMini.style.display=collapsed?'flex':'none';
    const miniName=document.getElementById('bld-mini-name');
    if(miniName){
     const b=placesRef.find(item=>item.id===selectedId);
     miniName.textContent=b?b.name:'未选择';
    }
   };
   document.getElementById('bld-collapse-btn').onclick=()=>setCollapse(true);
   document.getElementById('bld-expand-btn').onclick=()=>setCollapse(false);

   // 平移按钮绑定 (北=-Z, 南=+Z, 西=-X, 东=+X)
   document.getElementById('bld-up').onclick=()=>nudge(0, -stepMove, 0);
   document.getElementById('bld-down').onclick=()=>nudge(0, stepMove, 0);
   document.getElementById('bld-left').onclick=()=>nudge(-stepMove, 0, 0);
   document.getElementById('bld-right').onclick=()=>nudge(stepMove, 0, 0);

   // 旋转按钮绑定
   document.getElementById('bld-rot-ccw').onclick=()=>nudge(0, 0, -stepRot);
   document.getElementById('bld-rot-cw').onclick=()=>nudge(0, 0, stepRot);

   // 步长切换
   const setStepMove=(s, elId)=>{
    stepMove=s;
    ['bld-step-05','bld-step-1','bld-step-3'].forEach(id=>{
     const el=document.getElementById(id);
     el.style.background=(id===elId)?'#215e48':'#e5ece8';
     el.style.color=(id===elId)?'white':'#284d40';
    });
   };
   document.getElementById('bld-step-05').onclick=()=>setStepMove(0.5, 'bld-step-05');
   document.getElementById('bld-step-1').onclick=()=>setStepMove(1.0, 'bld-step-1');
   document.getElementById('bld-step-3').onclick=()=>setStepMove(3.0, 'bld-step-3');

   // 旋转步长切换
   const setStepRot=(r, elId)=>{
    stepRot=r;
    ['bld-rot-1','bld-rot-5','bld-rot-15'].forEach(id=>{
     const el=document.getElementById(id);
     el.style.background=(id===elId)?'#215e48':'#e5ece8';
     el.style.color=(id===elId)?'white':'#284d40';
    });
   };
   document.getElementById('bld-rot-1').onclick=()=>setStepRot(1, 'bld-rot-1');
   document.getElementById('bld-rot-5').onclick=()=>setStepRot(5, 'bld-rot-5');
   document.getElementById('bld-rot-15').onclick=()=>setStepRot(15, 'bld-rot-15');

   // 控制操作
   document.getElementById('bld-reset-cur').onclick=resetCurrent;
   document.getElementById('bld-reset-all').onclick=resetAll;
   document.getElementById('bld-export').onclick=exportTransformsCode;
   document.getElementById('bld-save').onclick=saveTransforms;
   document.getElementById('bld-close-btn').onclick=toggleEditor;
  }

  function toggleEditor(){
   isEditing=!isEditing;
   const hud=document.getElementById('building-editor-hud');
   const btn=document.getElementById('building-editor-btn');
   if(hud)hud.style.display=isEditing?'block':'none';
   if(btn){
    btn.classList.toggle('active', isEditing);
    btn.textContent=isEditing?'完成':'移楼';
   }
   if(highlightMarker)highlightMarker.visible=isEditing&&Boolean(selectedId);

   // 隐藏干扰 UI
   const toHide=['.top','.filters','.badge','.card','#labels','.hint'];
   toHide.forEach(sel=>{
    const el=document.querySelector(sel);
    if(el)el.style.display=isEditing?'none':'';
   });

   if(isEditing){
    // 进入移楼模式：自动切换实景底图，方便直接对着航拍图微调
    if(window.setGroundMode)window.setGroundMode('aerial');
    const modeBtn=document.getElementById('mode');
    if(modeBtn){modeBtn.textContent='实景';modeBtn.classList.add('active');}
    if(!selectedId&&placesRef.length>0){
     selectBuilding(placesRef[0].id);
    }
    showToast('已进入物体微调模式：点击任意建筑或场地，使用方向键平移旋转');
   }else{
    // 退出编辑模式：恢复所有物体默认颜色
    for(const [gId, g] of groupsRef){
     g.traverse(m=>{
      if(m.isMesh&&m.material&&m.material.emissive){
       m.material.emissive.set('#000000');
      }
     });
    }
   }
  }

  root.BuildingEditor={
   init,
   toggleEditor,
   isEditing:()=>isEditing,
   selectBuilding,
   nudge,
   getTransforms:()=>transforms,
   applyAllTransforms
  };
 }

 if(typeof module!=='undefined')module.exports=createBuildingEditor;
 else createBuildingEditor();
})(typeof window!=='undefined'?window:globalThis);
