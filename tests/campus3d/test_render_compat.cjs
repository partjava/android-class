const assert=require('node:assert/strict');
const {createRenderer}=require('../../app/src/main/assets/campus3d/scene-utils.js');
const attempts=[];
class Renderer {
 constructor(options){attempts.push(options);if(options.antialias)throw new Error('Multisampling unavailable');this.shadowMap={};this.capabilities={isWebGL2:false};}
 setPixelRatio(value){this.ratio=value;}
 setSize(w,h){this.size=[w,h];}
}
const THREE={WebGLRenderer:Renderer,PCFSoftShadowMap:2,SRGBColorSpace:'srgb'};
const renderer=createRenderer(THREE,420,780,3);
assert.equal(attempts.length,2);
assert.equal(attempts[1].antialias,false);
assert.equal(renderer.shadowMap.enabled,false);
assert.equal(renderer.ratio,1);
assert.deepEqual(renderer.size,[420,780]);
assert.equal(renderer.outputColorSpace,'srgb');
class ModernRenderer extends Renderer {
 constructor(options){super({...options,antialias:false});this.capabilities.isWebGL2=true;}
}
const modern=createRenderer({...THREE,WebGLRenderer:ModernRenderer},720,1280,2);
assert.equal(modern.ratio,1.6);
assert.equal(modern.shadowMap.enabled,true);
assert.equal(createRenderer({...THREE,WebGLRenderer:ModernRenderer},720,1280,undefined).ratio,1);
assert.throws(()=>createRenderer({...THREE,WebGLRenderer:class {constructor(){throw new Error('No GPU');}}},420,780,1),/No GPU/);
console.log('WebGL initialization fallback, quality limits and unavailable GPU checks passed');
