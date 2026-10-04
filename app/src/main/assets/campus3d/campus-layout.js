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
 {width:6,points:[[68,310],[345,198],[751,43],[1088,43],[1290,1085],[1075,1098],[704,1100],[140,1011],[62,542],[68,310]]},
 {width:4,points:[[197,276],[190,389],[191,554],[326,549],[322,904],[410,939],[531,941]]},
 {width:6,points:[[337,197],[362,408],[347,521],[423,798],[503,997],[480,1260]]},
 {width:6,points:[[751,43],[754,319],[819,558],[895,851],[962,1139]]},
 {width:4,points:[[58,407],[363,396],[571,450],[710,433],[751,452],[1087,368]]},
 {width:2.5,points:[[483,447],[578,421],[711,431],[751,458],[801,566],[831,724],[701,772],[593,769],[445,651],[397,588],[430,500],[483,447]]},
 {width:4,points:[[120,800],[454,810],[615,824],[683,809],[859,750],[1170,716]]},
 {width:5,points:[[140,1011],[504,1037],[704,1100],[1075,1098],[1290,1085]]},
 {width:2.3,points:[[930,481],[990,713]]},
 {width:2.3,points:[[830,601],[1087,580]]},
 {width:3,points:[[1030,715],[1070,919],[1105,1055]]},
 {width:3,points:[[926,928],[1200,874]]},
 {width:3,points:[[922,56],[924,165],[933,222],[970,346]]},
 {width:3,points:[[785,184],[909,175],[945,175],[1065,165]]}
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
