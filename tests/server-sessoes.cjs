const assert=require('node:assert/strict');const {randomUUID}=require('node:crypto');
const base=process.env.GOVSIM_TEST_URL||'http://localhost:18765';
async function req(path,id,body,method){const r=await fetch(base+path,{method:method||(body?'POST':'GET'),headers:{'Content-Type':'application/json',...(id?{'X-GovSim-Session':id}:{})},...(body?{body:JSON.stringify(body)}:{})});return {status:r.status,data:await r.json(),cookie:r.headers.get('set-cookie')};}
(async()=>{const a=randomUUID(),b=randomUUID();
 const [x,y]=await Promise.all([req('/api/iniciar',a,{modo:'normal',turnos:0,dificuldade:'MEDIO',seed:42}),req('/api/iniciar',b,{modo:'normal',turnos:0,dificuldade:'DIFICIL',seed:99})]);
 assert.equal(x.data.totalTurnos,12);assert.equal(y.data.totalTurnos,15);
 assert.equal(x.data.eventoAtual.id,'EV_POSSE');assert.equal(x.data.epilogoMandato,'');assert.deepEqual(x.data.licoesMandato,[]);
 assert.equal(typeof x.data.eventoAtual.decretoA.emPoucasPalavras,'string');assert.ok(x.data.eventoAtual.decretoA.emPoucasPalavras.split(/\s+/).length<=14);
 assert.equal(typeof x.data.dialogoApresentado,'string');assert.deepEqual(x.data.memoriaPersonagens,{});
 await req('/api/decidir',a,{decreto:'A',turno:1});
 const memoriaA=(await req('/api/estado',a)).data.memoriaPersonagens;assert.equal(Object.keys(memoriaA).length,1);const pessoa=Object.values(memoriaA)[0];assert.equal(pessoa.seguiu+pessoa.ignorou,1);assert.deepEqual((await req('/api/estado',b)).data.memoriaPersonagens,{});
 assert.equal((await req('/api/estado',a)).data.turnoAtual,2);assert.equal((await req('/api/estado',b)).data.turnoAtual,1);
 assert.equal((await req('/api/decidir',a,{decreto:'B',turno:1})).status,409);
 assert.equal((await req('/api/decidir',a,{decreto:'Z',turno:2})).status,400);
 assert.equal((await req('/api/decidir',a,null,'GET')).status,405);
 assert.equal((await req('/api/estado',a,{},'POST')).status,405);
 const cookie=await req('/api/estado');assert.ok(cookie.cookie.includes('HttpOnly'));
 const r=await fetch(base+'/api/estado',{headers:{Cookie:cookie.cookie.split(';')[0]}});assert.equal(r.headers.get('x-govsim-session'),cookie.cookie.split(';')[0].slice(7));
 const vocab=await fetch(base+'/glossario.json');assert.equal(vocab.status,200);const entradas=await vocab.json();for(const id of ['pib','caixa','aprovacao','inflacao','selic'])assert.ok(entradas.some(e=>e.id===id&&e.definicao&&e.comoMuda));
 const font=await fetch(base+'/fonts/geist-mono-latin.woff2');assert.equal(font.status,200);assert.equal(font.headers.get('content-type'),'font/woff2');
 const demo=randomUUID();let d=(await req('/api/iniciar',demo,{modo:'demo',turnos:4,dificuldade:'FACIL'})).data;
 for(let t=1;t<=4&&!d.partidaEncerrada;t++){if(t===4)assert.equal(d.eventoAtual.id,'EV_PRESTACAO_CONTAS');d=(await req('/api/decidir',demo,{decreto:t===2?'B':'A',turno:d.turnoAtual})).data;assert.ok(d.manchete);}
 assert.ok(d.partidaEncerrada);assert.ok(d.epilogoMandato);assert.ok(d.resultadoEleicao);assert.ok(d.licoesMandato.length>=2&&d.licoesMandato.length<=3);assert.match(d.licoesMandato[0],/Turno [0-9]+/);
 console.log('APROVADO: sessões isoladas, cookie, headers, HTTP 400/405/409 e fonte offline.');
})().catch(e=>{console.error(e);process.exitCode=1;});
