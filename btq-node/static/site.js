'use strict';
const COINS=[
 {id:'amrith',name:'Amrith',ticker:'AMRITH',color:'#DE8A3C',tag:'Nectar of the ledger.',lore:'AMRITH is the first pour. Immortal units on mortal phones — held before holding was cool, never rugged, never left the corner.'},
 {id:'kiaan',name:'Kiaan',ticker:'KIAAN',color:'#B34A2E',tag:'The king\'s share.',lore:'Forged at the lowest block heights and proud of it. KIAAN is proof the trenches do not just test a token — they crown one.'},
 {id:'crazy',name:'Alesha',ticker:'ALESHA',color:'#C93C74',tag:'Named Alesha.',lore:'ALESHA does not follow the ledger. The ledger follows ALESHA. Five billion units, zero chill.'},
 {id:'boujie',name:'BoujieClique',ticker:'BOUJIE',color:'#95507A',tag:'Champagne mechanics.',lore:'Old-money manners on new-money rails. Every BOUJIE transfer arrives dressed for dinner — and never pays the whale tax.'},
 {id:'qmile',name:'QuarterMile',ticker:'QMILE',color:'#3F8F7B',tag:'Fastest on the block.',lore:'A quarter mile of pure gas on blocks that seal in about eight seconds. QMILE does not race — it leaves.'},
 {id:'sof',name:'Bonn',ticker:'BONN',color:'#7D9CC0',tag:'Named Bonn.',lore:'No stress. No whale tax. No hard feelings. BONN takes the easy road on the same blocks and still arrives first.'},
 {id:'5ave',name:'5thAvenue',ticker:'5AVE',color:'#9AA66B',tag:'Stack now, flex later.',lore:'The disciplined one of the family. 5AVE turns mined units into five billion reasons, one sealed block at a time.'},
 {id:'thiro',name:'Thiro',ticker:'THIRO',color:'#E7A9A0',tag:'Third flame.',lore:'THIRO was not launched — it was adopted. Third of the renamed triad, still the main character of every group chat on the mesh.'},
 {id:'santi',name:'Santi',ticker:'SANTI',color:'#C4A35A',tag:'Peace on the tip.',lore:'SANTI seals last and rests first. Nine genesis L2s, one calm corner of the same blocks — peace that still stacks.'}
];
const GLYPHS={
 amrith:'<path d="M9.6 14.4l4.8-4.8"/><path d="M8.7 12.5l-2.3 2.3a3.3 3.3 0 0 0 4.7 4.7l2.3-2.3"/><path d="M15.3 11.5l2.3-2.3a3.3 3.3 0 0 0-4.7-4.7l-2.3 2.3"/>',
 kiaan:'<path d="M4 20.5h7"/><path d="M5.5 18.5L17.5 6.5"/><path d="M11.5 6.5h6v6"/>',
 crazy:'<path d="M13 2.5L5.5 13.5h5.2L9.4 21.5l9.1-12h-5.6l2.1-7z"/>',
 boujie:'<path d="M4 18h16"/><path d="M4.6 18L3.4 8.2l4.9 3.4L12 5.2l3.7 6.4 4.9-3.4L19.4 18"/>',
 qmile:'<rect x="5.5" y="3.5" width="13" height="10" fill="currentColor" fill-opacity=".14"/><path d="M5.5 21.5V3"/><path d="M5.5 8.5h13M11.9 3.5v10"/>',
 sof:'<path d="M19.5 4.5C12 5 6.8 10.5 5.2 19.3 13.8 18.5 18.8 12.5 19.5 4.5z"/><path d="M19.5 4.5L6.5 18"/>',
 '5ave':'<ellipse cx="12" cy="6.3" rx="6.4" ry="2.7"/><path d="M5.6 6.3v5.2c0 1.5 2.9 2.7 6.4 2.7s6.4-1.2 6.4-2.7V6.3"/><path d="M5.6 11.5v5.2c0 1.5 2.9 2.7 6.4 2.7s6.4-1.2 6.4-2.7v-5.2"/>',
 thiro:'<path d="M12 20.4S4 15.4 3.1 10.3C2.7 7.6 4.7 5.2 7.4 5.2c1.9 0 3.6 1.1 4.6 2.8 1-1.7 2.7-2.8 4.6-2.8 2.7 0 4.7 2.4 4.3 5.1-.9 5.1-8.9 10.1-8.9 10.1z"/>',
 santi:'<circle cx="12" cy="12" r="7.2"/><path d="M12 6.5v11M6.5 12h11"/><circle cx="12" cy="12" r="2.2"/>'
};
const glyph=(id,color,size)=>`<svg viewBox="0 0 24 24" width="${size}" height="${size}" fill="none" stroke="${color}" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" style="color:${color}">${GLYPHS[id]}</svg>`;
const LOTUS=(s=24)=>{let p='';for(let i=0;i<8;i++)p+=`<ellipse cx="12" cy="5.9" rx="2.5" ry="3.9" transform="rotate(${i*45} 12 12)"/>`;
 return `<svg viewBox="0 0 24 24" width="${s}" height="${s}" fill="none" stroke="currentColor" stroke-width="1.4">${p}<circle cx="12" cy="12" r="2.3"/></svg>`;};
document.querySelectorAll('.mark').forEach(m=>m.innerHTML=LOTUS(+m.dataset.s||26));

const fmt=n=>n.toLocaleString('en-US');
const fmtAmt=x=>Number.isInteger(x)?fmt(x):String(parseFloat(x.toFixed(6)));
function toast(msg){const t=document.createElement('div');t.className='toast';t.textContent=msg;
 document.getElementById('toasts').appendChild(t);
 setTimeout(()=>{t.classList.add('out');setTimeout(()=>t.remove(),320);},3600);}
function copyText(txt,msg){
 const done=()=>toast(msg||'Copied to clipboard.');
 const fb=()=>{const ta=document.createElement('textarea');ta.value=txt;ta.style.position='fixed';ta.style.opacity='0';
  document.body.appendChild(ta);ta.select();try{document.execCommand('copy');done();}catch(e){toast('Copy failed — select it manually.');}ta.remove();};
 if(navigator.clipboard&&navigator.clipboard.writeText){navigator.clipboard.writeText(txt).then(done).catch(fb);}else fb();}

const VIEWS=['home','coin','nine','rewards','network','wallet','join'];
const TITLES={home:'BRAHMNETWORK — Creation is mined.',coin:'Brahma Coin — BRAHMNETWORK',nine:'The Genesis Nine — BRAHMNETWORK',
 rewards:'Adoption & Referrals — BRAHMNETWORK',network:'Network & Launch Gates — BRAHMNETWORK',wallet:'Brahma Coin Wallet — BRAHMNETWORK',join:'Join the Testnet — BRAHMNETWORK'};
let currentView='home';
function route(){
 let h=location.hash.replace(/^#\/?/,'').split('?')[0];
 if(!VIEWS.includes(h))h='home';
 document.querySelectorAll('.view').forEach(v=>v.classList.remove('on'));
 document.getElementById('view-'+h).classList.add('on');
 document.querySelectorAll('.nav a').forEach(a=>a.classList.toggle('active',a.getAttribute('href')==='#/'+h));
 document.title=TITLES[h];
 window.scrollTo({top:0,left:0,behavior:'instant'});
 if(h!=='wallet')pauseMining();
 currentView=h;
 if(h==='nine')initEightFocus();
 closeMenu();
}
addEventListener('hashchange',route);

const burger=document.getElementById('burger'),mmenu=document.getElementById('mmenu');
function closeMenu(){burger.classList.remove('open');mmenu.classList.remove('open');document.body.style.overflow='';}
burger.addEventListener('click',()=>{const o=!mmenu.classList.contains('open');
 burger.classList.toggle('open',o);mmenu.classList.toggle('open',o);document.body.style.overflow=o?'hidden':'';});
mmenu.querySelectorAll('a').forEach(a=>a.addEventListener('click',closeMenu));

(function(){
 const items=[{name:'Brahma Coin',ticker:'NATIVE',supply:'100,000,000',color:'#E4AC3F',g:LOTUS(17)}]
  .concat(COINS.map(c=>({name:c.name,ticker:c.ticker,supply:'5,000,000,000',color:c.color,g:glyph(c.id,c.color,17)})));
 const html=items.map(o=>`<span class="tk">${o.g}<b>${o.name}</b><span>${o.ticker}</span><u>· ${o.supply} UNITS</u></span>`).join('');
 document.getElementById('tkTrack').innerHTML=html+html;
})();

(function(){
 const cv=document.getElementById('orbitCanvas');if(!cv)return;
 const ctx=cv.getContext('2d'),wrap=document.getElementById('orbWrap'),tip=document.getElementById('orbTip');
 let W=600,H=520,cx=300,cy=260,S=1;
 function resize(){const r=wrap.getBoundingClientRect();W=r.width;H=r.height;
  const dpr=Math.min(2,window.devicePixelRatio||1);
  cv.width=W*dpr;cv.height=H*dpr;ctx.setTransform(dpr,0,0,dpr,0,0);
  cx=W/2;cy=H/2;S=Math.min(W,H)/600;}
 resize();addEventListener('resize',resize);
 const nodes=COINS.map((c,i)=>({id:c.id,ticker:c.ticker,name:c.name,color:c.color,
  a:i/8*Math.PI*2+0.7,r:70+i*27,sp:(0.16+((i*7)%5)*0.03)*(i%2?-1:1),sz:5+((i*5)%3)}));
 let tilt=0.42,rot=-0.55,vr=0,drag=false,moved=0,lx=0,ly=0,hov=-1;
 const pt={x:-9999,y:-9999};
 const pos=n=>{const A=n.a+rot;return{x:cx+Math.cos(A)*n.r*S,y:cy+Math.sin(A)*n.r*S*tilt,d:Math.sin(A)};};
 function drawNode(n,p,front){const hovM=(hov===n.id)?1.7:1;
  const r=n.sz*S*(0.8+0.35*((p.d+1)/2))*hovM;
  ctx.globalAlpha=front?1:0.45;
  ctx.beginPath();ctx.arc(p.x,p.y,r,0,Math.PI*2);ctx.fillStyle=n.color;ctx.fill();
  ctx.beginPath();ctx.arc(p.x,p.y,r+3.5,0,Math.PI*2);ctx.strokeStyle='rgba(13,11,8,.9)';ctx.lineWidth=2;ctx.stroke();
  if(front&&p.d>0.25){ctx.font='500 9px "IBM Plex Mono"';ctx.fillStyle=n.color;ctx.globalAlpha=.85;
   ctx.textAlign='center';ctx.fillText(n.ticker,p.x,p.y+r+14);}
  ctx.globalAlpha=1;}
 function drawCore(){const R=26*Math.max(.72,S);
  ctx.beginPath();ctx.arc(cx,cy,R,0,Math.PI*2);ctx.fillStyle='#E4AC3F';ctx.fill();
  ctx.beginPath();ctx.arc(cx,cy,R+6,0,Math.PI*2);ctx.strokeStyle='rgba(228,172,63,.32)';ctx.lineWidth=1;ctx.stroke();
  ctx.strokeStyle='rgba(20,15,6,.55)';ctx.lineWidth=1.4;
  for(let i=0;i<8;i++){const a=i*Math.PI/4+rot*0.4;
   ctx.beginPath();ctx.moveTo(cx+Math.cos(a)*R*.3,cy+Math.sin(a)*R*.3);
   ctx.lineTo(cx+Math.cos(a)*R*.78,cy+Math.sin(a)*R*.78);ctx.stroke();}
  ctx.beginPath();ctx.arc(cx,cy,R*.14,0,Math.PI*2);ctx.fillStyle='rgba(20,15,6,.75)';ctx.fill();
  ctx.font='500 9px "IBM Plex Mono"';ctx.fillStyle='rgba(228,172,63,.9)';ctx.textAlign='center';
  ctx.fillText('B R A H M A',cx,cy+R+26);}
 let last=performance.now();
 function frame(t){requestAnimationFrame(frame);
  const dt=Math.min(.05,(t-last)/1000);last=t;
  if(currentView!=='home')return;
  nodes.forEach(n=>n.a+=n.sp*dt);
  if(!drag){rot+=0.05*dt+vr;vr*=0.94;}
  ctx.clearRect(0,0,W,H);
  ctx.lineWidth=1;ctx.setLineDash([2,6]);ctx.strokeStyle='rgba(228,204,160,.1)';
  for(const n of nodes){ctx.beginPath();ctx.ellipse(cx,cy,n.r*S,n.r*S*tilt,0,0,Math.PI*2);ctx.stroke();}
  ctx.setLineDash([]);
  const order=nodes.map(n=>({n,p:pos(n)})).sort((a,b)=>a.p.d-b.p.d);
  for(const o of order)if(o.p.d<0)drawNode(o.n,o.p,false);
  drawCore();
  for(const o of order)if(o.p.d>=0)drawNode(o.n,o.p,true);
  if(drag){tip.style.opacity=0;return;}
  let best=null,bd=1e18;
  for(const n of nodes){const p=pos(n);const d2=(p.x-pt.x)**2+(p.y-pt.y)**2;if(d2<bd){bd=d2;best={n,p};}}
  const hit=bd<Math.pow(17+S*6,2)?best:null;
  hov=hit?hit.n.id:null;
  wrap.classList.toggle('pt',!!hit);
  if(hit){tip.style.opacity=1;tip.style.left=hit.p.x+'px';tip.style.top=(hit.p.y-16)+'px';
   tip.innerHTML=`<b>${hit.n.name}</b><span>5,000,000,000 UNITS · LAYER 2 ON THE SAME BLOCKS</span>`;}
  else tip.style.opacity=0;}
 requestAnimationFrame(frame);
 const setPt=e=>{const r=cv.getBoundingClientRect();pt.x=e.clientX-r.left;pt.y=e.clientY-r.top;};
 cv.addEventListener('pointerdown',e=>{drag=true;moved=0;lx=e.clientX;ly=e.clientY;setPt(e);cv.setPointerCapture(e.pointerId);});
 cv.addEventListener('pointermove',e=>{setPt(e);
  if(drag){const dx=e.clientX-lx,dy=e.clientY-ly;lx=e.clientX;ly=e.clientY;
   moved+=Math.abs(dx)+Math.abs(dy);rot+=dx*0.0045;vr=dx*0.0009;
   tilt=Math.max(.16,Math.min(.9,tilt+dy*0.0026));}});
 cv.addEventListener('pointerup',()=>{drag=false;
  if(moved<6&&hov){sessionStorage.setItem('bn-coin',hov);location.hash='#/nine';}});
 cv.addEventListener('pointerleave',()=>{pt.x=-9999;pt.y=-9999;});
})();

let cur=0;
const rail=document.getElementById('rail'),detail=document.getElementById('detail');
rail.innerHTML=COINS.map((c,i)=>`<button class="rail-item" data-i="${i}"><i>${String(i+1).padStart(2,'0')}</i>${glyph(c.id,c.color,18)}<b>${c.name}</b><span>${c.ticker}</span></button>`).join('');
function renderDetail(){
 const c=COINS[cur];
 detail.style.setProperty('--coin',c.color);
 detail.innerHTML=`
  <div class="d-top"><span class="d-idx">${String(cur+1).padStart(2,'0')} / 08 — L2 ON THE SAME BLOCKS</span>
   <div class="d-nav">
    <button data-dnav="-1" aria-label="Previous token"><svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"><path d="M19 12H5M11 6l-6 6 6 6"/></svg></button>
    <button data-dnav="1" aria-label="Next token"><svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"><path d="M5 12h14M13 6l6 6-6 6"/></svg></button>
   </div></div>
  <div class="d-swap">
   <div class="d-main"><span class="d-glyph">${glyph(c.id,c.color,78)}</span>
    <div><h3 class="d-name">${c.name}</h3><div class="d-rule"></div><p class="d-tag">${c.tag}</p></div></div>
   <p class="d-lore">${c.lore}</p>
   <div class="d-specs">
    <div><i>GENESIS GRANT</i><b>5,000,000,000</b></div>
    <div><i>LAYER</i><b>2 ON L1</b></div>
    <div><i>PER HALVING DROP</i><b>750,000,000</b></div>
    <div><i>WHALE TAX</i><b>EXEMPT</b></div>
   </div>
   <p class="d-note">CREATED BY A ONE-TIME OPERATOR GRANT TO A GENESIS L2 ADDRESS.<br>WALLETS THAT HAVE JOINED OR MINED RECEIVE 750,000,000 WHOLE UNITS AT EVERY MINE-SCHEDULE HALVING.<br>LAUNCH MORE: BURN 100 BRAHMA COIN · UNIQUE ALPHANUMERIC SYMBOL · BURN IS PERMANENT.</p>
  </div>`;
 const sw=detail.querySelector('.d-swap');sw.style.animation='none';void sw.offsetWidth;sw.style.animation='';
}
function selectCoin(i){cur=(i+8)%8;renderDetail();
 [...rail.children].forEach((el,j)=>el.classList.toggle('on',j===cur));}
rail.addEventListener('click',e=>{const b=e.target.closest('.rail-item');if(b)selectCoin(+b.dataset.i);});
detail.addEventListener('click',e=>{const b=e.target.closest('[data-dnav]');if(b)selectCoin(cur+ +b.dataset.dnav);});
addEventListener('keydown',e=>{if(currentView!=='nine')return;
 if(e.key==='ArrowRight')selectCoin(cur+1);
 if(e.key==='ArrowLeft')selectCoin(cur-1);});
function initEightFocus(){const id=sessionStorage.getItem('bn-coin');
 if(id){const i=COINS.findIndex(c=>c.id===id);if(i>=0)selectCoin(i);sessionStorage.removeItem('bn-coin');}}
selectCoin(0);

const K256=new Uint32Array([
0x428a2f98,0x71374491,0xb5c0fbcf,0xe9b5dba5,0x3956c25b,0x59f111f1,0x923f82a4,0xab1c5ed5,
0xd807aa98,0x12835b01,0x243185be,0x550c7dc3,0x72be5d74,0x80deb1fe,0x9bdc06a7,0xc19bf174,
0xe49b69c1,0xefbe4786,0x0fc19dc6,0x240ca1cc,0x2de92c6f,0x4a7484aa,0x5cb0a9dc,0x76f988da,
0x983e5152,0xa831c66d,0xb00327c8,0xbf597fc7,0xc6e00bf3,0xd5a79147,0x06ca6351,0x14292967,
0x27b70a85,0x2e1b2138,0x4d2c6dfc,0x53380d13,0x650a7354,0x766a0abb,0x81c2c92e,0x92722c85,
0xa2bfe8a1,0xa81a664b,0xc24b8b70,0xc76c51a3,0xd192e819,0xd6990624,0xf40e3585,0x106aa070,
0x19a4c116,0x1e376c08,0x2748774c,0x34b0bcb5,0x391c0cb3,0x4ed8aa4a,0x5b9cca4f,0x682e6ff3,
0x748f82ee,0x78a5636f,0x84c87814,0x8cc70208,0x90befffa,0xa4506ceb,0xbef9a3f7,0xc67178f2]);
const KW=new Uint32Array(64);
function sha256Hex(str){
 const enc=new TextEncoder().encode(str);
 const len=enc.length,total=((len+9+63)>>6)<<6;
 const buf=new Uint8Array(total);buf.set(enc);buf[len]=0x80;
 const bl=len*8;
 buf[total-4]=(bl>>>24)&255;buf[total-3]=(bl>>>16)&255;buf[total-2]=(bl>>>8)&255;buf[total-1]=bl&255;
 const out=sha256Core(buf,total);
 return out.map(w=>(w>>>0).toString(16).padStart(8,'0')).join('');
}
function sha256Core(buf,total){
 let h0=0x6a09e667|0,h1=0xbb67ae85|0,h2=0x3c6ef372|0,h3=0xa54ff53a|0,
     h4=0x510e527f|0,h5=0x9b05688c|0,h6=0x1f83d9ab|0,h7=0x5be0cd19|0;
 for(let off=0;off<total;off+=64){
  for(let i=0;i<16;i++){const o=off+(i<<2);KW[i]=((buf[o]<<24)|(buf[o+1]<<16)|(buf[o+2]<<8)|buf[o+3])>>>0;}
  for(let i=16;i<64;i++){
   const x=KW[i-15],y=KW[i-2];
   const s0=((x>>>7)|(x<<25))^((x>>>18)|(x<<14))^(x>>>3);
   const s1=((y>>>17)|(y<<15))^((y>>>19)|(y<<13))^(y>>>10);
   KW[i]=(KW[i-16]+s0+KW[i-7]+s1)|0;
  }
  let a=h0,b=h1,c=h2,d=h3,e=h4,f=h5,g=h6,hh=h7;
  for(let i=0;i<64;i++){
   const S1=((e>>>6)|(e<<26))^((e>>>11)|(e<<21))^((e>>>25)|(e<<7));
   const ch=(e&f)^(~e&g);
   const t1=(hh+S1+ch+K256[i]+KW[i])|0;
   const S0=((a>>>2)|(a<<30))^((a>>>13)|(a<<19))^((a>>>22)|(a<<10));
   const mj=(a&b)^(a&c)^(b&c);
   const t2=(S0+mj)|0;
   hh=g;g=f;f=e;e=(d+t1)|0;d=c;c=b;b=a;a=(t1+t2)|0;
  }
  h0=(h0+a)|0;h1=(h1+b)|0;h2=(h2+c)|0;h3=(h3+d)|0;
  h4=(h4+e)|0;h5=(h5+f)|0;h6=(h6+g)|0;h7=(h7+hh)|0;
 }
 return [h0,h1,h2,h3,h4,h5,h6,h7];
}

const MINER=(function(){
 const balEl=document.getElementById('simBal'),nextEl=document.getElementById('nextMine'),
  rewEl=document.getElementById('rewNow'),netHEl=document.getElementById('netH'),netJEl=document.getElementById('netJ'),
  eyeL1=document.getElementById('eyeL1'),eyeL2=document.getElementById('eyeL2'),simL2=document.getElementById('simL2'),tgtEl=document.getElementById('tgtEl'),
  ring=document.getElementById('ringP'),lzBig=document.getElementById('lzBig'),
  attTxt=document.getElementById('attTxt'),attTgt=document.getElementById('attTgt'),
  rateEl=document.getElementById('rateEl'),nonceEl=document.getElementById('nonceEl'),
  foundEl=document.getElementById('foundEl'),logEl=document.getElementById('mineLog'),
  startBtn=document.getElementById('mineStart'),zone=document.getElementById('bbZone'),
  scr=document.getElementById('scr'),selfEl=document.getElementById('selfTest');
 const CIRC=2*Math.PI*95;
 const BASE='BRAH1|mine|BRM1G8K2P9VXQ4M7T3JH5LNC6YRFDUAWESZGQ7DF|';
 const baseBytes=new Uint8Array(BASE.length);
 for(let i=0;i<BASE.length;i++)baseBytes[i]=BASE.charCodeAt(i);
 const BL=baseBytes.length;
 const scratch=new Uint8Array(160);scratch.set(baseBytes);
 let padLen=-1,padTotal=64;
 let bits=20,joins=8000,mining=false,nonce=Math.floor(Math.random()*900000)+100000,
  attempts=0,found=0,bal=0,l2sum=0,firstDone=false,hideL1=false,hideL2=false,rateEma=0,timer=null;
 const rewardFor=j=>j<=10000?0.00014:0.00014/Math.pow(2,Math.ceil((j-10000)/100000));
 function lzForNonce(n){
  const s=''+n,len=BL+s.length;
  for(let j=0;j<s.length;j++)scratch[BL+j]=s.charCodeAt(j);
  if(len!==padLen){
   padTotal=((len+9+63)>>6)<<6;
   scratch.fill(0,len+1,padTotal);
   const bl=len*8;
   scratch[padTotal-4]=(bl>>>24)&255;scratch[padTotal-3]=(bl>>>16)&255;
   scratch[padTotal-2]=(bl>>>8)&255;scratch[padTotal-1]=bl&255;
   padLen=len;
  }
  scratch[len]=0x80;
  const h=sha256Core(scratch,padTotal);
  let lz,w=h[0]>>>0;
  if(w===0){lz=32;w=h[1]>>>0;lz+=(w===0)?32:Math.clz32(w);}
  else lz=Math.clz32(w);
  return lz;
 }
 function draw(){
  balEl.textContent=hideL1?'•••••':bal.toFixed(5);
  if(simL2)simL2.textContent=hideL2?'•••••':String(Math.floor(l2sum));
  rewEl.textContent=fmtAmt(rewardFor(joins))+' / BLOCK';
  const next=firstDone?rewardFor(joins):rewardFor(joins)+1;
  nextEl.textContent=firstDone?('+'+fmtAmt(next)+' / BLOCK'):('+'+fmtAmt(next)+' · FIRST MINE');
  netHEl.textContent=fmt(netH);netJEl.textContent=fmt(netJ);
  const target=Math.pow(2,bits);
  tgtEl.textContent='2^'+bits;
  attTgt.textContent=fmt(target);
  ring.style.strokeDashoffset=CIRC*(1-Math.min(1,attempts/target));
 }
 function log(msg){const d=document.createElement('div');d.className='new';d.textContent=msg;
  logEl.prepend(d);while(logEl.children.length>4)logEl.lastChild.remove();
  setTimeout(()=>d.classList.remove('new'),1400);}
 function onBlock(){
  found++;netH++;
  const reward=firstDone?rewardFor(joins):rewardFor(joins)+1;
  firstDone=true;bal+=reward;
  log(`BLOCK #${found} SEALED · +${fmtAmt(reward)} BRAHMA COIN${reward>=1?' · FIRST MINE':''}`);
  const rip=document.createElement('span');rip.className='rip';zone.appendChild(rip);
  setTimeout(()=>rip.remove(),720);
  scr.classList.remove('found');void scr.offsetWidth;scr.classList.add('found');
  attempts=0;draw();
 }
 function slice(){
  if(!mining)return;
  const target=Math.pow(2,bits);
  const t0=performance.now();
  let n=0,hit=false,lz=0;
  const batch=Math.max(800,Math.min(40000,Math.round((rateEma||150000)*0.012)));
  while(n<batch){
   lz=lzForNonce(nonce);n++;attempts++;nonce++;
   if(lz>=bits){hit=true;break;}
  }
  const dt=Math.max(1,performance.now()-t0);
  rateEma=rateEma*0.7+(n/dt*1000)*0.3;
  rateEl.textContent=fmt(Math.round(rateEma));
  nonceEl.textContent=fmt(nonce);
  lzBig.textContent=String(lz).padStart(2,'0');
  attTxt.textContent=fmt(attempts);
  if(hit)onBlock();
  draw();
  if(mining)timer=setTimeout(slice,0);
 }
 function start(){
  mining=true;startBtn.textContent='HALT MINER';startBtn.classList.add('on');
  log(`SEARCHING 2^${bits} — TARGET ${bits} LEADING ZERO BITS`);
  slice();
 }
 function pause(){
  mining=false;clearTimeout(timer);
  startBtn.textContent='START MINER';startBtn.classList.remove('on');
 }
 startBtn.addEventListener('click',()=>{mining?pause():start();});
 document.querySelectorAll('.bitb').forEach(b=>b.addEventListener('click',()=>{
  document.querySelectorAll('.bitb').forEach(x=>x.classList.remove('on'));
  b.classList.add('on');bits=+b.dataset.b;attempts=0;
  if(mining)log(`DIFFICULTY → ${bits} BITS · TARGET ${fmt(Math.pow(2,bits))}`);
  draw();}));
 document.querySelectorAll('.jb').forEach(b=>b.addEventListener('click',()=>{
  document.querySelectorAll('.jb').forEach(x=>x.classList.remove('on'));
  b.classList.add('on');joins=+b.dataset.j;
  log(`SIM NETWORK → ~${fmt(joins)} JOINED · BLOCK PAYS ${fmtAmt(rewardFor(joins))}`);
  draw();}));
 eyeL1.addEventListener('click',()=>{hideL1=!hideL1;eyeL1.classList.toggle('on',hideL1);draw();
  toast(hideL1?'L1 Brahma Coin hidden.':'L1 Brahma Coin visible.');});
 eyeL2.addEventListener('click',()=>{hideL2=!hideL2;eyeL2.classList.toggle('on',hideL2);draw();
  toast(hideL2?'L2 memecoins hidden.':'L2 memecoins visible.');});
 let netH=1024809,netJ=8412;
 setInterval(()=>{if(currentView!=='wallet')return;
  netH+=1+(Math.random()<.2?1:0);
  if(Math.random()<.3)netJ++;
  netHEl.textContent=fmt(netH);netJEl.textContent=fmt(netJ);},8000);
 (function(){
  const a=sha256Hex('abc')==='ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad';
  const b=sha256Hex('')==='e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855';
  selfEl.textContent=a&&b?'SHA-256 SELF-TEST — PASS (THIS TAB HASHES FOR REAL)':'SHA-256 SELF-TEST — FAIL';
  if(a&&b)selfEl.classList.add('pass');
 })();
 draw();
 return {pause};
})();
function pauseMining(){MINER.pause();}

document.querySelectorAll('.ptab').forEach(t=>t.addEventListener('click',()=>{
 document.querySelectorAll('.ptab').forEach(x=>x.classList.remove('on'));
 t.classList.add('on');
 document.getElementById('pt-home').classList.toggle('hidden',t.dataset.pt!=='home');
 document.getElementById('pt-mine').classList.toggle('hidden',t.dataset.pt!=='mine');}));

(function(){
 const T=[[1,10000,'TIER I — FIRST HANDS'],[10001,35000,'TIER II — SECOND WAVE'],
  [35001,60000,'TIER III — THIRD WATCH'],[60001,85000,'TIER IV — LAST QUARTER']];
 const range=document.getElementById('calcRange'),num=document.getElementById('calcNum'),
  eEra=document.getElementById('cEra'),eW=document.getElementById('cWallet'),
  eR=document.getElementById('cRef'),eM=document.getElementById('cMeta'),
  mark=document.getElementById('youMark');
 function tierFor(n){
  if(n<=10000)return{t:'TIER I — FIRST HANDS',j:100,lo:1,hi:10000};
  if(n<=35000)return{t:'TIER II — SECOND WAVE',j:50,lo:10001,hi:35000};
  const k=Math.max(0,Math.floor((n-35001)/25000));
  const lo=35001+k*25000;
  const hi=lo+24999;
  const names={0:'TIER III — THIRD WATCH',1:'TIER IV — LAST QUARTER'};
  return{t:names[k]||('TAIL — HALVED ×'+k),j:25/Math.pow(2,k),lo,hi};
 }
 function calc(v){
  v=Math.max(1,Math.min(300000,Math.round(v)||1));
  const t=tierFor(v),r=t.j/2;
  eEra.textContent=t.t;
  eW.textContent=fmtAmt(t.j);
  eR.textContent=fmtAmt(r);
  const win=(v>85000)?`${fmt(t.lo)} – ${fmt(t.hi)} (TAIL CONTINUES)`:`${fmt(t.lo)} – ${fmt(Math.min(t.hi,85000))}`;
  eM.innerHTML=`TIER WINDOW — JOINS ${win}<br>REFERRER RECEIVES HALF · NODE LIMITS JOINS TO 20 / HOUR · PAID FROM THE 25,000,000 POOL`;
  if(v<=85000){
   const i=v<=10000?0:v<=35000?1:v<=60000?2:3;
   const frac=i*0.25+((v-T[i][0])/(T[i][1]-T[i][0]))*0.25;
   mark.style.display='';mark.style.left=(frac*100)+'%';
  }else mark.style.display='none';
 }
 range.addEventListener('input',()=>{num.value=range.value;calc(+range.value);});
 num.addEventListener('input',()=>{range.value=num.value;calc(+num.value);});
 calc(12400);
})();

(function(){
 const inp=document.getElementById('invAddr'),btn=document.getElementById('invGen'),
  out=document.getElementById('invOut'),
  webEl=document.getElementById('invWeb'),brahmEl=document.getElementById('invBrahm');
 let links={web:'',brahm:''};
 function parseAddr(raw){
  const s=(raw||'').trim();
  const m=s.match(/^(BRM1G|BTQ1G)([0-9a-fA-F]+)$/);
  if(!m)return null;
  const rest=m[2].toLowerCase();
  if(rest.length!==24&&rest.length!==28)return null;
  const body=rest.slice(0,24);
  const chk=sha256Hex('BRM1G'+body).slice(0,4);
  return 'BRM1G'+body+chk;
 }
 btn.addEventListener('click',()=>{
  const addr=parseAddr(inp.value);
  if(!addr){
   toast('That does not look like a BRAHMNETWORK address — BRM1G plus 24 or 28 hex digits, lowercase body.');inp.focus();return;}
  links={web:`/claim?ref=${addr}`,
   brahm:`brahm://claim?ref=${addr}`};
  webEl.textContent=location.origin+links.web;brahmEl.textContent=links.brahm;
  out.classList.add('show');
  toast('Claim links built — referrers receive half the join amount.');});
 document.querySelectorAll('[data-inv]').forEach(b=>b.addEventListener('click',()=>{
  const k=b.dataset.inv;
  const txt=k==='web'?(location.origin+links.web):links[k];
  if(txt)copyText(txt,'Link copied — go spread it.');}));
})();

document.querySelectorAll('.fq-q').forEach(q=>q.addEventListener('click',()=>{
 const fq=q.parentElement,open=fq.classList.contains('open');
 document.querySelectorAll('.fq').forEach(f=>f.classList.remove('open'));
 if(!open)fq.classList.add('open');}));
document.querySelectorAll('[data-toast]').forEach(b=>b.addEventListener('click',e=>{
 e.preventDefault();toast(b.dataset.toast);}));
document.getElementById('fYear').textContent=new Date().getFullYear();
setInterval(()=>{document.getElementById('utcClock').textContent=new Date().toUTCString().slice(17,25)+' UTC';},1000);
setInterval(()=>{const el=document.getElementById('phClock');
 if(el)el.textContent=new Date().toTimeString().slice(0,5);},15000);
const io=new IntersectionObserver(es=>es.forEach(x=>{
 if(x.isIntersecting){x.target.classList.add('in');io.unobserve(x.target);}}),{threshold:.12});
document.querySelectorAll('.rv').forEach(el=>io.observe(el));

(function(){
 fetch('/',{method:'POST',headers:{'Content-Type':'application/json'},
  body:JSON.stringify({jsonrpc:'2.0',id:1,method:'brah_getStats',params:[]})})
 .then(r=>r.json()).then(j=>{
  const s=j&&j.result;if(!s)return;
  const set=(id,t)=>{const e=document.getElementById(id);if(e)e.textContent=t;};
  if(s.chain_height!=null)set('liveHeight',fmt(s.chain_height));
  if(s.joined_count!=null)set('liveJoined',fmt(s.joined_count));
  const pl=!!s.public_launch;
  ['liveLaunch','gateLaunch'].forEach(id=>{
   const el=document.getElementById(id);if(!el)return;
   el.textContent=pl?'TRUE':'FALSE';
   el.classList.toggle('ok',pl);el.classList.toggle('bad',!pl);
  });
  const chip=document.getElementById('gateChip');
  if(chip)chip.textContent=pl?'PUBLIC LAUNCH — TRUE':'PRIVATE TESTNET — PUBLIC LAUNCH: FALSE';
 }).catch(()=>{});
})();
route();
