const assert=require('node:assert/strict');const {randomUUID}=require('node:crypto');
const base=process.env.GOVSIM_TEST_URL||'http://localhost:18765';
async function req(path,id,body,method){const r=await fetch(base+path,{method:method||(body?'POST':'GET'),headers:{'Content-Type':'application/json',...(id?{'X-GovSim-Session':id}:{})},...(body?{body:JSON.stringify(body)}:{})});return {status:r.status,data:await r.json(),cookie:r.headers.get('set-cookie')};}
(async()=>{const a=randomUUID(),b=randomUUID();
 const [x,y]=await Promise.all([req('/api/iniciar',a,{modo:'normal',turnos:0,dificuldade:'MEDIO',seed:42}),req('/api/iniciar',b,{modo:'normal',turnos:0,dificuldade:'DIFICIL',seed:99})]);
 assert.equal(x.data.totalTurnos,12);assert.equal(y.data.totalTurnos,15);
 await req('/api/decidir',a,{decreto:'A',turno:1});
 assert.equal((await req('/api/estado',a)).data.turnoAtual,2);assert.equal((await req('/api/estado',b)).data.turnoAtual,1);
 assert.equal((await req('/api/decidir',a,{decreto:'B',turno:1})).status,409);
 assert.equal((await req('/api/decidir',a,{decreto:'Z',turno:2})).status,400);
 assert.equal((await req('/api/decidir',a,null,'GET')).status,405);
 assert.equal((await req('/api/estado',a,{},'POST')).status,405);
 const cookie=await req('/api/estado');assert.ok(cookie.cookie.includes('HttpOnly'));
 const r=await fetch(base+'/api/estado',{headers:{Cookie:cookie.cookie.split(';')[0]}});assert.equal(r.headers.get('x-govsim-session'),cookie.cookie.split(';')[0].slice(7));
 const font=await fetch(base+'/fonts/geist-mono-latin.woff2');assert.equal(font.status,200);assert.equal(font.headers.get('content-type'),'font/woff2');
 console.log('APROVADO: sessões isoladas, cookie, headers, HTTP 400/405/409 e fonte offline.');
})().catch(e=>{console.error(e);process.exitCode=1;});
