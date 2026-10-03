(function(root){
function roofVertices(w,d,rise){return [-w/2,0,-d/2,w/2,0,-d/2,0,rise,-d/2,-w/2,0,d/2,w/2,0,d/2,0,rise,d/2];}
function chooseLabels(items){const accepted=[];for(const item of [...items].sort((a,b)=>b.priority-a.priority)){const r=item.rect;if(!accepted.some(a=>r[0]<a.rect[2]+6&&r[2]+6>a.rect[0]&&r[1]<a.rect[3]+4&&r[3]+4>a.rect[1]))accepted.push(item);}return accepted.map(a=>a.id);}
const api={roofVertices,chooseLabels};if(typeof module!=='undefined')module.exports=api;else root.SceneUtils=api;
})(typeof window!=='undefined'?window:globalThis);
