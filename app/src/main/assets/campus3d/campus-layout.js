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
boundary:[[52,308],[332,187],[753,30],[1090,32],[1285,1065],[1220,1090],[1180,1255],[1070,1255],[994,1100],[715,1110],[630,1068],[630,1210],[431,1210],[431,1080],[155,1029],[8,1029],[0,795],[62,542]],
lake:[[549,456],[657,437],[718,473],[747,561],[766,591],[803,650],[800,702],[749,723],[674,734],[587,699],[562,657],[536,614],[530,532]],
grass:[[819,479],[1045,464],[1117,690],[868,746]],
plaza:[[614,1065],[1016,1090],[1070,1198],[750,1210],[644,1150]],
track:[[623,99],[675,102],[715,133],[732,181],[735,347],[719,400],[675,426],[604,427],[562,409],[544,372],[540,182],[550,143],[580,115]],
pitch:[[576,155],[698,153],[703,365],[574,365]],
courts:[[[15,828],[83,820],[98,1000],[31,1010]],[[605,814],[781,787],[802,916],[630,935]],[[452,1094],[604,1100],[608,1200],[441,1190]]],
roads:[
 // 1. 南门迎宾主轴与南部横向迎宾路
 {width:6.5,points:[[818,1240],[818,1085]]},
 {width:6.0,points:[[818,1210],[1070,1210],[1180,1255]]},
 {width:6.0,points:[[818,1210],[630,1210],[430,1210]]},
 {width:4.0,points:[[630,1210],[630,1050]]},
 {width:4.5,points:[[495,1210],[495,1050]]},

 // 2. 东西横贯主干大道 (完全在建筑物南侧外围主街通行，绝不穿墙)
 {width:6.0,points:[[115,1035],[290,1035],[495,1045],[540,1050],[640,1050],[670,1050],[818,1085],[920,1085],[1070,1085],[1260,1085]]},

 // 3. 晴川广场核心路网
 {width:4.8,points:[[670,1050],[670,740]]},
 {width:4.8,points:[[920,1085],[920,935],[880,915],[880,680]]},
 {width:5.0,points:[[670,740],[780,730],[880,680]]},

 // 4. 教学区 (教1-4) 外环主道 (100% 沿建筑外墙外围道路，完全避开墙体)
 {width:5.0,points:[[880,680],[1040,670],[1250,660]]},
 {width:5.0,points:[[920,1085],[1070,1085],[1260,1085]]},
 {width:5.0,points:[[880,680],[880,915],[920,935],[920,1085]]},
 {width:5.0,points:[[1250,660],[1260,1085]]},
 {width:3.5,points:[[880,915],[940,915]]},
 {width:3.5,points:[[1250,895],[1260,895]]},

 // 5. 情缘湖 360° 环湖大道与滨水观光路
 {width:4.8,points:[[530,730],[620,755],[700,755],[780,730],[805,690]]},
 {width:4.8,points:[[805,690],[805,620],[765,540],[725,470]]},
 {width:4.8,points:[[725,470],[650,440],[550,440]]},
 {width:4.8,points:[[550,440],[470,550],[470,660],[530,730]]},
 {width:4.5,points:[[700,755],[670,740]]},

 // 6. 东侧玉屏大道/光谷六路辅道 (校园东边界主通道)
 {width:6.0,points:[[1250,30],[1260,350],[1275,700],[1285,1065],[1285,1150]]},

 // 7. 科技实验与综合办公区 (实1, 实2, 综1, 综2)
 {width:4.8,points:[[760,40],[930,40],[1110,40]]},
 {width:4.8,points:[[760,375],[930,375],[1110,375]]},
 {width:4.8,points:[[760,40],[760,195],[760,375]]},
 {width:4.8,points:[[1110,40],[1110,195],[1110,375]]},
 {width:3.8,points:[[760,195],[830,195]]},
 {width:3.8,points:[[920,195],[940,195]]},
 {width:3.8,points:[[1035,195],[1110,195]]},
 {width:3.8,points:[[930,40],[930,195]]},
 {width:4.0,points:[[780,375],[780,450],[930,450],[1080,450],[1080,375]]},

 // 8. 体育场馆区 (操场环路与体育馆周边)
 {width:4.5,points:[[520,85],[735,85]]},
 {width:4.5,points:[[520,440],[725,440]]},
 {width:4.5,points:[[735,85],[725,440]]},
 {width:4.5,points:[[520,85],[520,440]]},
 {width:4.2,points:[[360,140],[360,385]]},
 {width:4.2,points:[[360,140],[520,140]]},
 {width:4.2,points:[[360,385],[520,385]]},

 // 9. 宿舍生活区多级路网 (完全依随楼间空地通道行走)
 {width:4.0,points:[[50,240],[190,200],[300,175],[350,175]]},
 {width:3.5,points:[[50,400],[70,400]]},
 {width:4.0,points:[[50,545],[200,545],[350,545]]},
 {width:3.5,points:[[195,405],[195,540]]},
 {width:5.0,points:[[350,175],[350,380],[350,545],[350,680],[420,680],[420,805]]},
 {width:3.5,points:[[75,725],[105,725]]},
 {width:3.5,points:[[75,805],[130,805]]},
 {width:3.8,points:[[322,805],[322,930]]},
 {width:3.2,points:[[115,930],[135,930]]},
 {width:3.2,points:[[460,940],[490,940]]},
 {width:4.5,points:[[495,805],[495,1045]]},

 // 10. 西侧商业街/小吃街 (完全沿外沿边缘道路)
 {width:4.5,points:[[50,240],[50,400],[50,545],[75,555],[75,715],[95,795],[115,925],[115,1035]]},

 // 11. 食堂、超市、小剧场联络线
 {width:4.0,points:[[495,890],[540,890]]},
 {width:4.0,points:[[645,890],[670,890]]},
 {width:4.2,points:[[920,935],[920,1085]]}
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
