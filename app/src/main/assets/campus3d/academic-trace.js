/* Six buildings and parking traced from the user's 942 x 2048 annotation.
 * A common transform preserves recesses, attachments and relative proportions.
 * Unmarked main courtyard edges are read from the accompanying unmarked photo.
 * Existing unconfirmed floor estimates for art/computer buildings are retained. */
(function(root){
const shapes={
 lab_1:{outer:[[95,584],[377,608],[368,806],[360,867],[310,858],[260,884],[260,846],[169,846],[164,884],[73,878]],holes:[[[143,676],[304,693],[293,779],[137,761]]],roofWings:[[[95,584],[377,608],[304,693],[143,676]],[[95,584],[143,676],[137,761],[73,878]],[[377,608],[368,806],[360,867],[293,779],[304,693]],[[137,761],[293,779],[260,846],[169,846]],[[137,761],[169,846],[164,884],[73,878]],[[293,779],[360,867],[310,858],[260,846]]],links:[{outline:[[373,749],[437,745],[436,809],[368,806]],height:8}]},
 lab_2:{outer:[[430,580],[740,584],[727,821],[649,827],[652,874],[591,881],[590,827],[437,817]],holes:[[[493,662],[659,664],[670,751],[487,751]]],roofWings:[[[430,580],[740,584],[659,664],[493,662]],[[740,584],[727,821],[670,751],[659,664]],[[430,580],[493,662],[487,751],[437,817]],[[437,817],[487,751],[670,751],[727,821],[649,827],[590,827]],[[590,827],[649,827],[652,874],[591,881]]]},
 art_museum:{outer:[[73,878],[169,846],[260,846],[310,858],[288,1138],[231,1140],[233,1027],[200,1028],[202,1064],[146,1064],[145,1008],[85,1006],[85,949],[67,946]],holes:[[[172,854],[253,854],[253,876],[172,876]]],roofWings:[[[85,949],[298,912],[288,1019],[145,1008]]]},
 computer_center:{outer:[[528,901],[595,881],[591,838],[649,827],[652,874],[682,871],[724,984],[697,987],[706,1052],[616,1054],[611,1018],[551,1024]],holes:[],roofWings:[[[528,901],[595,881],[682,871],[724,984],[551,1024]],[[551,1024],[724,984],[697,987],[706,1052],[616,1054],[611,1018]]],links:[{outline:[[616,1054],[706,1052],[709,1070],[621,1084]],height:7.5}]},
 admin_1:{outer:[[300,1127],[723,1041],[787,1261],[483,1333],[459,1255],[489,1245],[464,1166],[380,1187],[369,1164]],holes:[[[523,1159],[677,1129],[693,1204],[543,1232]]],roofWings:[[[300,1127],[723,1041],[677,1129],[523,1159],[464,1166],[380,1187],[369,1164]],[[723,1041],[787,1261],[693,1204],[677,1129]],[[787,1261],[483,1333],[543,1232],[693,1204]],[[483,1333],[459,1255],[489,1245],[464,1166],[523,1159],[543,1232]]]},
 admin_2:{outer:[[75,1155],[300,1127],[369,1164],[425,1350],[145,1418],[118,1399]],holes:[[[166,1234],[316,1203],[336,1287],[184,1327]]],roofWings:[[[75,1155],[300,1127],[316,1203],[166,1234]],[[300,1127],[369,1164],[425,1350],[336,1287],[316,1203]],[[425,1350],[145,1418],[184,1327],[336,1287]],[[145,1418],[118,1399],[75,1155],[166,1234],[184,1327]]]}
};
// 单个建筑位置微调表：[左右X, 上下Z] (正数往右/往下，负数往左/往上)
const offsets={
 lab_1:[0,0],          // 实1 (左上)
 lab_2:[0,0],          // 实2 (右上)
 admin_1:[0,0],        // 综1 (右下)
 admin_2:[0,0],        // 综2 (左下)
 art_museum:[0,0],     // 连廊 (左)
 computer_center:[0,0] // 连廊 (右)
};
const trace={reference:{width:942,height:2048,source:'User green-line laboratory/comprehensive-area annotation'},shapes,offsets,world:(p,id)=>{
 const off=(id&&offsets[id])||[0,0];
 return [201+(p[0]-95)/10+off[0],20+(p[1]-584)/10+off[1]];
},parking:[[166,1469],[733,1362],[766,1369],[772,1490],[736,1546],[222,1657]],parkingGrid:[[180,1470],[748,1385],[740,1537],[231,1640]]};
trace.parkingWorld=trace.parking.map(p=>trace.world(p));
trace.buildParking=function({scene,flat,mat}){
 flat(trace.parkingWorld,.3,'#828d89');
 const line=points=>{const mesh=new THREE.Line(new THREE.BufferGeometry().setFromPoints(points.map(p=>trace.world(p)).map(p=>new THREE.Vector3(p[0],.34,p[1]))),new THREE.LineBasicMaterial({color:'#ede6d2'}));scene.add(mesh);};
 line([...trace.parking,trace.parking[0]]);
 const q=trace.parkingGrid,p=(u,v)=>CampusLayout.bilinear(q,u,v);
 for(let r=0;r<4;r++){const a=.06+r*.23,b=a+.14;line([p(.03,a),p(.97,a)]);for(let c=0;c<=22;c++){const u=.03+c*.94/22;line([p(u,a),p(u,b)]);}}
};
trace.apply=function(data,models){for(const [id,shape]of Object.entries(shapes)){
 const b=data.buildings.find(b=>b.id===id),m=models[id],points=shape.outer.map(p=>trace.world(p,id)),xs=points.map(p=>p[0]),zs=points.map(p=>p[1]);
 b.x=(Math.min(...xs)+Math.max(...xs))/2;b.z=(Math.min(...zs)+Math.max(...zs))/2;
 const local=p=>{const q=trace.world(p,id);return [q[0]-b.x,q[1]-b.z];};
 m.outer=shape.outer.map(local);m.holes=shape.holes.map(loop=>loop.map(local));m.roofWings=shape.roofWings.map(loop=>loop.map(local));m.links=(shape.links||[]).map(a=>({outline:a.outline.map(local),height:a.height}));
 m.roof='traced';m.infill=[];m.annex=[];m.corners=[];m.source='User-marked laboratory/comprehensive-area screenshot';
}};
if(typeof module!=='undefined')module.exports=trace;else {root.AcademicTrace=trace;trace.apply(root.CampusData,root.CampusFootprints);}
})(typeof window!=='undefined'?window:globalThis);
