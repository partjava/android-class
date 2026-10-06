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
 // 1. 南门迎宾主轴与南部横向迎宾路 (晴川大门主入口)
 {width:6.5,points:[[818,1240],[818,1090]]},
 {width:6.0,points:[[818,1210],[1070,1210],[1180,1255]]},
 {width:6.0,points:[[818,1210],[630,1210],[430,1210]]},
 {width:4.0,points:[[630,1210],[630,1070]]},
 {width:4.5,points:[[480,1210],[480,1035]]},

 // 2. 东西横贯主干大道 (串联生活区、中央广场与教学区)
 {width:6.0,points:[[140,1020],[340,1020],[480,1035],[640,1040],[704,1090],[818,1090],[900,1090],[1075,1090],[1220,1090]]},

 // 3. 晴川广场核心路网与礼仪大道
 {width:4.8,points:[[704,1090],[704,740]]},
 {width:4.8,points:[[900,1090],[900,740]]},
 {width:5.0,points:[[704,740],[818,730],[900,740]]},
 {width:4.0,points:[[644,1150],[750,1210]]},

 // 4. 教1-教4 教学组团多级路网
 {width:5.0,points:[[900,710],[1060,695],[1220,690]]},
 {width:5.0,points:[[900,1070],[1060,1070],[1220,1070]]},
 {width:5.0,points:[[900,710],[900,1070]]},
 {width:5.0,points:[[1220,690],[1220,1070]]},
 {width:4.0,points:[[900,895],[1060,895],[1220,895]]},
 {width:4.0,points:[[1060,695],[1060,895],[1060,1070]]},
 {width:3.2,points:[[920,830],[1040,830]]},
 {width:3.2,points:[[1080,830],[1200,830]]},
 {width:3.2,points:[[920,960],[1040,960]]},
 {width:3.2,points:[[1080,960],[1200,960]]},

 // 5. 情缘湖环湖观光大道 (全线闭合环路)
 {width:4.8,points:[[530,730],[600,750],[680,755],[760,735],[800,705]]},
 {width:4.8,points:[[800,705],[810,640],[770,560],[730,470]]},
 {width:4.8,points:[[730,470],[660,435],[570,440],[530,470]]},
 {width:4.8,points:[[530,470],[520,550],[515,630],[530,730]]},
 {width:3.6,points:[[515,630],[450,630],[450,540],[520,520]]},
 {width:4.5,points:[[680,755],[704,740]]},

 // 6. 东侧玉屏大道/光谷六路辅路 (校园东边界主通道)
 {width:6.0,points:[[1250,30],[1260,350],[1275,700],[1285,1065],[1285,1150]]},

 // 7. 综1-综2-实1-实2 科技实验与综合办公区路网
 {width:4.8,points:[[760,45],[930,45],[1100,45]]},
 {width:4.8,points:[[760,365],[930,365],[1100,365]]},
 {width:4.8,points:[[760,45],[760,195],[760,365]]},
 {width:4.8,points:[[1100,45],[1100,195],[1100,365]]},
 {width:4.0,points:[[760,195],[930,195],[1100,195]]},
 {width:4.0,points:[[930,45],[930,195],[930,365]]},
 {width:4.0,points:[[780,365],[780,450],[930,450],[1080,450],[1080,365]]},
 {width:4.0,points:[[760,365],[730,470]]},

 // 8. 体育场馆区 (操场环路与体育馆周边)
 {width:4.5,points:[[520,85],[740,85]]},
 {width:4.5,points:[[520,435],[660,435],[740,435]]},
 {width:4.5,points:[[740,85],[740,435]]},
 {width:4.5,points:[[520,85],[520,435]]},
 {width:4.2,points:[[370,170],[520,170]]},
 {width:4.2,points:[[370,370],[520,370]]},
 {width:4.2,points:[[370,170],[370,370]]},
 {width:4.0,points:[[520,260],[540,260]]},

 // 9. 宿舍生活区核心主干道与分支网
 {width:5.5,points:[[340,240],[340,390],[340,540],[340,690],[340,810],[340,930],[340,1020]]},
 {width:4.0,points:[[60,250],[340,250]]},
 {width:4.0,points:[[60,390],[340,390]]},
 {width:4.0,points:[[60,540],[340,540]]},
 {width:4.0,points:[[80,690],[340,690]]},
 {width:4.0,points:[[80,795],[340,795]]},
 {width:4.0,points:[[120,810],[340,810],[480,810]]},
 {width:4.0,points:[[120,930],[340,930],[480,930]]},
 {width:4.0,points:[[120,1020],[340,1020],[480,1020]]},
 {width:4.0,points:[[480,810],[480,1035]]},

 // 10. 西侧商业街/小吃街
 {width:4.5,points:[[60,250],[60,540]]},
 {width:4.5,points:[[60,540],[80,560],[80,795]]},
 {width:4.5,points:[[80,795],[120,810],[120,1020]]},

 // 11. 食堂、超市、小剧场、球场、驾校联络线
 {width:4.2,points:[[480,890],[640,890]]},
 {width:4.2,points:[[640,890],[640,1040]]},
 {width:4.0,points:[[640,950],[704,950],[900,950]]},
 {width:4.0,points:[[340,690],[450,690],[530,730]]},
 {width:4.0,points:[[730,470],[900,500],[1100,500],[1275,500]]}
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
