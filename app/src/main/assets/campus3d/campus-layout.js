/* Hand traced on one satellite overview. Pixel coordinates deliberately retained
 * for comparison with the source; world coordinates use one uniform 1:4 scale.
 * Not survey data. Roof edges and courtyards are approximate. */
(function(root){
const placements={};
function place(id,quad){placements[id]={quad,center:[0,1].map(k=>quad.reduce((s,p)=>s+p[k],0)/4)};}
place('teach_1',[[1050,710],[1130,697],[1167,880],[1080,907]]);
place('teach_3',[[895,755],[994,729],[1033,874],[930,907]]);
place('teach_2',[[1090,930],[1202,900],[1248,1035],[1121,1062]]);
place('teach_4',[[947,944],[1052,924],[1091,1057],[982,1074]]);
place('lab_1',[[775,53],[900,64],[909,173],[790,183]]);
place('lab_2',[[942,48],[1060,57],[1064,164],[937,173]]);
place('admin_2',[[782,236],[915,216],[949,340],[810,365]]);
place('admin_1',[[950,246],[1053,230],[1091,329],[987,360]]);
place('art_museum',[[839,163],[894,153],[913,200],[855,212]]);
place('computer_center',[[955,169],[1005,157],[1027,204],[975,216]]);
place('dorm_10',[[60,306],[145,262],[183,359],[81,398]]);
place('dorm_9',[[202,260],[302,224],[335,337],[226,370]]);
place('dorm_8',[[72,414],[165,401],[184,522],[79,541]]);
place('dorm_7',[[202,402],[317,381],[340,510],[221,541]]);
place('dorm_6',[[93,578],[268,553],[303,662],[105,716]]);
place('dorm_5',[[110,715],[277,688],[310,778],[118,800]]);
place('dorm_4',[[132,822],[298,793],[315,892],[140,927]]);
place('dorm_1',[[329,829],[448,811],[487,932],[345,952]]);
place('dorm_3',[[151,941],[272,926],[303,996],[159,1008]]);
place('dorm_2',[[295,950],[435,965],[455,1027],[303,1027]]);
place('small_dorm',[[330,704],[377,693],[408,804],[362,823]]);
place('canteen_main',[[545,899],[642,901],[638,1030],[545,1032]]);
place('theater_market',[[700,959],[858,954],[899,1021],[716,1039]]);
place('gym',[[370,172],[485,146],[512,367],[391,377]]);
placements.library={center:[508,604],outline:[[465,460],[510,473],[541,550],[514,611],[502,632],[551,675],[580,704],[560,744],[500,714],[451,662],[462,622],[485,584],[469,550],[436,533]]};
const layout={reference:{source:'全景2.jpg',resized:[942,2048],crop:[130,730,790,1360],width:1320,height:1260,pixelsPerUnit:4},placements,
boundary:[[10,200],[150,180],[330,120],[600,40],[750,20],[1100,20],[1270,20],[1310,30],[1310,400],[1310,750],[1310,1100],[1310,1258],[1180,1258],[818,1258],[430,1258],[10,1258],[10,1050],[10,750],[10,450]],
lake:[[549,456],[657,437],[718,473],[747,561],[766,591],[803,650],[800,702],[749,723],[674,734],[587,699],[562,657],[536,614],[530,532]],
grass:[[819,479],[1045,464],[1117,690],[868,746]],
plaza:[[614,1065],[1016,1090],[1070,1198],[750,1210],[644,1150]],
track:[[623,99],[675,102],[715,133],[732,181],[735,347],[719,400],[675,426],[604,427],[562,409],[544,372],[540,182],[550,143],[580,115]],
pitch:[[576,155],[698,153],[703,365],[574,365]],
courts:[[[15,828],[83,820],[98,1000],[31,1010]],[[605,814],[781,787],[802,916],[630,935]],[[452,1094],[604,1100],[608,1200],[441,1190]]],
roads:[
 // 1. 南门迎宾主轴与南部横向迎宾路 (侨兴街与主入口广场)
 {width:6.5,points:[[818,1245],[818,1140]]},
 {width:6.0,points:[[818,1210],[1070,1210],[1180,1255]]},
 {width:6.0,points:[[818,1210],[630,1210],[430,1210],[90,1210]]},
 {width:4.5,points:[[630,1210],[630,1070]]},
 {width:4.5,points:[[480,1210],[480,1070]]},

 // 2. 东西横贯主干大道 (完全顺应实景白线南侧主通道，0穿墙)
 {width:6.0,points:[[90,1050],[290,1050],[480,1065],[630,1070],[670,1070],[818,1088],[920,1088],[1090,1088],[1265,1088],[1285,1088]]},

 // 3. 晴川广场核心礼仪大道与中轴连线 (避让小剧场)
 {width:5.0,points:[[670,1070],[670,940],[670,750]]},
 {width:5.0,points:[[920,1088],[920,940],[875,750],[875,680]]},
 {width:4.5,points:[[670,940],[920,940]]},
 {width:5.0,points:[[670,750],[750,750],[875,750]]},
 {width:5.5,points:[[818,940],[818,750]]},

 // 4. 教学区 (教1, 教2, 教3, 教4) 外围闭合通道 (完全绕行外侧，0侵入外墙)
 {width:5.0,points:[[875,680],[1040,680],[1265,680]]},
 {width:5.0,points:[[875,680],[875,915],[920,930],[920,1088]]},
 {width:5.0,points:[[1265,680],[1265,890],[1265,1088]]},
 {width:3.5,points:[[875,915],[920,915]]},
 {width:3.5,points:[[1215,915],[1265,915]]},

 // 5. 情缘湖 360° 环湖大道与湖滨步道 (完全依附实景水岸，0碰触图书馆)
 {width:4.5,points:[[550,435],[630,430],[700,450],[760,480]]},
 {width:4.5,points:[[760,480],[775,580],[765,660],[720,730],[670,750]]},
 {width:4.5,points:[[670,750],[590,755]]},
 {width:4.5,points:[[415,435],[415,600],[415,755],[480,755],[590,755]]},
 {width:4.0,points:[[415,435],[550,435]]},

 // 6. 校园东界光谷六路辅道 (东门主入口通道)
 {width:6.0,points:[[1130,60],[1250,30],[1260,360],[1275,680],[1285,1088],[1285,1255]]},
 {width:5.0,points:[[1275,680],[1265,680]]},

 // 7. 科技实验与综合办公区 (实1, 实2, 综1, 综2)
 {width:4.5,points:[[750,30],[925,30],[1095,30],[1130,60]]},
 {width:4.5,points:[[750,390],[925,390],[1115,390],[1260,390]]},
 {width:4.5,points:[[750,30],[750,205],[750,390],[760,480]]},
 {width:4.5,points:[[1095,30],[1115,60],[1115,205],[1115,390]]},
 {width:3.5,points:[[750,205],[830,205]]},
 {width:3.5,points:[[925,205],[950,205]]},
 {width:3.2,points:[[1038,205],[1115,205]]},
 {width:3.5,points:[[925,30],[925,205]]},
 {width:3.5,points:[[925,370],[925,390]]},

 // 8. 体育场馆区 (操场环路与体育馆周边)
 {width:4.5,points:[[345,140],[450,115],[600,60],[750,30]]},
 {width:4.2,points:[[345,140],[345,395]]},
 {width:4.5,points:[[345,395],[530,395],[540,435],[700,435],[750,390]]},
 {width:4.0,points:[[530,115],[530,395]]},

 // 9. 宿舍生活区多级路网 (1栋~10栋楼间清晰通道，0穿墙)
 {width:4.5,points:[[45,220],[45,400],[45,550],[65,560],[65,715],[85,800],[85,930],[90,1050]]},
 {width:4.0,points:[[45,220],[170,245],[220,205],[320,195],[345,140]]},
 {width:4.5,points:[[350,195],[350,370],[355,395],[355,550],[355,680],[425,680],[425,792],[505,792],[505,1065]]},
 {width:3.5,points:[[45,400],[65,400]]},
 {width:3.5,points:[[185,385],[195,385]]},
 {width:3.5,points:[[340,385],[355,385]]},
 {width:3.5,points:[[45,550],[75,555]]},
 {width:3.5,points:[[185,555],[195,555]]},
 {width:3.5,points:[[345,555],[355,555]]},
 {width:3.5,points:[[65,715],[85,715]]},
 {width:3.5,points:[[85,805],[110,805]]},
 {width:2.2,points:[[322,805],[322,930]]},
 {width:4.0,points:[[90,1050],[285,1050],[480,1050]]},

 // 10. 食堂、超市、小剧场周边联络线
 {width:4.2,points:[[505,885],[535,885]]},
 {width:4.2,points:[[650,885],[670,885]]},
 {width:4.0,points:[[505,792],[505,1065]]}
]};
layout.world=p=>p.map(v=>v/4);
layout.bilinear=(quad,u,v)=>[0,1].map(k=>(1-u)*(1-v)*quad[0][k]+u*(1-v)*quad[1][k]+u*v*quad[2][k]+(1-u)*v*quad[3][k]);
layout.calibrate=function(data,models){
 for(const b of data.buildings){const p=placements[b.id],m=models[b.id];b.x=p.center[0]/4;b.z=p.center[1]/4;
  if(p.outline){m.outer=p.outline.map(q=>[(q[0]-p.center[0])/4,(q[1]-p.center[1])/4]);continue;}
  const xs=m.outer.map(q=>q[0]),zs=m.outer.map(q=>q[1]),minX=Math.min(...xs),minZ=Math.min(...zs),w=Math.max(...xs)-minX,d=Math.max(...zs)-minZ;
  const convert=q=>{const a=layout.bilinear(p.quad,(q[0]-minX)/w,(q[1]-minZ)/d);return [(a[0]-p.center[0])/4,(a[1]-p.center[1])/4];};
  // Keep open courtyard roof wings as polygons, so skewed buildings do not acquire rectangular roofs.
  if(m.roof==='open'){const south=m.outer[1][0]>0,wing=w*.25;m.roofWings=[[[minX,minZ],[minX+wing,minZ],[minX+wing,minZ+d],[minX,minZ+d]],[[minX+w-wing,minZ],[minX+w,minZ],[minX+w,minZ+d],[minX+w-wing,minZ+d]],[[minX+wing,south?minZ:minZ+d-wing],[minX+w-wing,south?minZ:minZ+d-wing],[minX+w-wing,south?minZ+wing:minZ+d],[minX+wing,south?minZ+wing:minZ+d]]].map(loop=>loop.map(convert));}
  m.outer=m.outer.map(convert);m.holes=m.holes.map(loop=>loop.map(convert));m.corners=m.corners.map(convert);
  for(const a of [...m.infill,...m.annex]){a.outline=[[a.x-a.w/2,a.z-a.d/2],[a.x+a.w/2,a.z-a.d/2],[a.x+a.w/2,a.z+a.d/2],[a.x-a.w/2,a.z+a.d/2]].map(convert);const c=convert([a.x,a.z]);a.x=c[0];a.z=c[1];}
 }
};
if(typeof module!=='undefined')module.exports=layout;else {root.CampusLayout=layout;layout.calibrate(root.CampusData,root.CampusFootprints);}
})(typeof window!=='undefined'?window:globalThis);
