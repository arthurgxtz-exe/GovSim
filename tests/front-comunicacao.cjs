// Testa o código real de comunicação de app.js em VM, sem navegador ou bibliotecas.
const fs=require('node:fs'),vm=require('node:vm'),assert=require('node:assert/strict');
const elementos=new Map();
function elemento(id){if(!elementos.has(id)){const classes=new Set();elementos.set(id,{value:'FACIL',textContent:'',disabled:false,focus(){},classList:{add(...cs){cs.forEach(c=>classes.add(c));},remove(...cs){cs.forEach(c=>classes.delete(c));},contains(c){return classes.has(c);}}});}return elementos.get(id);}
let respostas=[], chamadas=[], pendentes=[], renderizados=[];
const sandbox={document:{addEventListener(){},getElementById:elemento,querySelector:()=>({inert:false})},window:{matchMedia:()=>({matches:true}),crypto:{randomUUID:()=> '00000000-0000-4000-8000-000000000001'}},console,performance,
 fetch:async(url,opcoes)=>{chamadas.push({url,opcoes});const proximo=respostas.shift();if(proximo instanceof Error)throw proximo;if(!proximo)throw new Error('Resposta de teste ausente');return proximo;},
 setTimeout:cb=>{pendentes.push(Promise.resolve().then(cb));return 1;},clearTimeout(){},globalThis:null};
sandbox.globalThis=sandbox;sandbox.renderizados=renderizados;
let fonte=fs.readFileSync('src/govsim/view/app.js','utf8');
const injecao=`
 tocarSom=function(){};resetarInclinacaoCarta=function(){};
 renderizarTudo=function(e){globalThis.renderizados.push(e);};
 atualizarPilaresERodape=function(){};preencherModalCronicas=function(){};exibirConsequenciaDoTurno=function(){};
 globalThis.teste={iniciarNovaPartida,executarDecisao,carregarEstadoInicial,estado:()=>estadoAtual,processando:()=>processando};
`;
const fim=fonte.lastIndexOf('})();');assert.ok(fim>0);fonte=fonte.slice(0,fim)+injecao+fonte.slice(fim);
vm.runInNewContext(fonte,sandbox);
function resposta(status,dados){return {ok:status>=200&&status<300,status,json:async()=>dados};}
function estado(turno){return {turnoAtual:turno,indicadores:{caixa:{}},partidaEncerrada:false};}
async function decidir(){sandbox.teste.executarDecisao('A');await Promise.all(pendentes.splice(0));}
(async()=>{
 const inicial=estado(1);respostas=[resposta(200,inicial)];await sandbox.teste.iniciarNovaPartida('normal',0);assert.equal(sandbox.teste.estado(),inicial);
 respostas=[resposta(400,{erro:'Dificuldade inválida'})];await sandbox.teste.iniciarNovaPartida('normal',0);
 assert.equal(sandbox.teste.estado(),inicial);assert.equal(renderizados.length,1);assert.match(elemento('erro-api').textContent,/Dificuldade inválida/);assert.equal(elemento('erro-api').classList.contains('hidden'),false);
 respostas=[new Error('Servidor indisponível')];await sandbox.teste.iniciarNovaPartida('normal',0);assert.equal(sandbox.teste.estado(),inicial);
 let inicio=chamadas.length;const remoto=estado(2);respostas=[resposta(409,{erro:'Turno duplicado'}),resposta(200,remoto)];await decidir();
 assert.equal(sandbox.teste.estado(),remoto);assert.deepEqual(chamadas.slice(inicio).map(c=>c.url),['/api/decidir','/api/estado']);assert.equal(sandbox.teste.processando(),false);
 inicio=chamadas.length;const aposRede=estado(3);respostas=[new Error('Resposta perdida'),resposta(200,aposRede)];await decidir();
 assert.equal(sandbox.teste.estado(),aposRede);assert.deepEqual(chamadas.slice(inicio).map(c=>c.url),['/api/decidir','/api/estado']);
 respostas=[new Error('Rede caiu'),new Error('Consulta indisponível')];await decidir();assert.equal(sandbox.teste.estado(),aposRede);assert.match(elemento('erro-api').textContent,/último estado conhecido/);assert.equal(sandbox.teste.processando(),false);
 inicio=chamadas.length;respostas=[resposta(400,{erro:'Decreto inválido'})];await decidir();assert.equal(sandbox.teste.estado(),aposRede);assert.equal(chamadas.length-inicio,1);
 respostas=[{status:200,ok:true,json:async()=>{throw new SyntaxError('JSON quebrado');}},resposta(200,estado(4))];await decidir();assert.equal(sandbox.teste.estado().turnoAtual,4);
 const final=estado(5);respostas=[resposta(200,final)];await decidir();assert.equal(sandbox.teste.estado(),final);assert.equal(elemento('erro-api').classList.contains('hidden'),true);
 assert.equal(respostas.length,0);
 console.log('APROVADO: front preserva estado em erro de início/400; ressincroniza em 409, rede e resposta ambígua, sem reenviar decisão; exibe erros e recupera controles.');
})().catch(e=>{console.error(e);process.exitCode=1;});

// Exercita os handlers reais: captura, movimento, retorno, cancelamento e assinatura.
{
 const handlers={},enviados=[],previews=[];let capturado=false;
 const carta={offsetWidth:300,style:{},classList:{add(){},remove(){}},addEventListener:(k,f)=>handlers[k]=f,setPointerCapture(){capturado=true;},hasPointerCapture:()=>capturado,releasePointerCapture(){capturado=false;}};
 const hover={};const arena={addEventListener:(k,f)=>hover[k]=f,getBoundingClientRect:()=>({left:0,width:500})};carta.closest=seletor=>{assert.equal(seletor,".card-arena");return arena;};
 const inclinacoes=[];
 const ctx={simularInclinacao:dx=>inclinacoes.push(dx),document:{getElementById:()=>carta},window:{addEventListener(){}},processando:false,animandoCarta:false,exibindoConsequencia:false,estadoAtual:{partidaEncerrada:false},arrastando:false,inicioX:0,deltaXAtual:0,reduzirMovimento:{matches:false},executarDecisao:c=>enviados.push(c),resetarInclinacaoCarta:()=>{carta.style.transform='';},atualizarCortinaEDots:dx=>previews.push(dx)};
 const real=fs.readFileSync('src/govsim/view/app.js','utf8');vm.createContext(ctx);vm.runInContext(real.slice(real.indexOf('    function configurarArrasteCarta()'),real.indexOf('    function simularInclinacao')),ctx);ctx.configurarArrasteCarta();
 hover.pointermove({pointerType:'mouse',clientX:40});assert.ok(inclinacoes.pop()<0);hover.pointermove({pointerType:'mouse',clientX:460});assert.ok(inclinacoes.pop()>0);hover.pointermove({pointerType:'touch',clientX:460});assert.equal(inclinacoes.length,0);hover.pointermove({pointerType:'mouse',clientX:250});assert.equal(carta.style.transform,'');hover.pointerleave();assert.equal(enviados.length,0);
 const ev=x=>({pointerId:1,button:0,clientX:x});
 handlers.pointerdown(ev(200));assert.equal(capturado,true);handlers.pointermove(ev(240));assert.match(carta.style.transform,/translateX\(40px\).*rotate\(4.8deg\)/);handlers.pointerup(ev(240));assert.equal(enviados.length,0);assert.equal(carta.style.transform,'');
 handlers.pointerdown(ev(200));handlers.pointermove(ev(80));handlers.pointerup(ev(80));assert.equal(enviados.pop(),'A');
 ctx.reduzirMovimento.matches=true;handlers.pointerdown(ev(200));handlers.pointermove(ev(320));assert.match(carta.style.transform,/rotate\(0deg\)/);handlers.pointerup(ev(320));assert.equal(enviados.pop(),'B');
 handlers.pointerdown(ev(200));handlers.pointercancel();assert.equal(ctx.arrastando,false);assert.ok(previews.length>=3);
 console.log('APROVADO: pointer capture, inclinação, limiar de 35%, A/B, retorno e cancelamento; movimento reduzido sem rotação.');
}
{
 const cartas=JSON.parse(fs.readFileSync('src/govsim/engine/eventos.json','utf8'));
 for(const c of cartas){assert.ok(c.dialogo.trim().split(/\s+/).length<=20,c.id);assert.ok(!/…|\.\.\.$/.test(c.dialogo),c.id);}
 console.log('APROVADO: 24 diálogos completos, sem truncamento, com até 20 palavras.');
}
{
 let criados=0,retomados=0,notas=0;const parametros=()=>({value:0,setValueAtTime(){},exponentialRampToValueAtTime(){}});
 class AudioFalso{constructor(){criados++;this.state='suspended';this.currentTime=0;this.destination={};}createGain(){return {gain:parametros(),connect(){}};}resume(){retomados++;return {catch(cb){cb(new Error('bloqueado'));}};}createOscillator(){notas++;return {frequency:parametros(),connect(){},start(){},stop(){}};}}
 const c={somAtivo:true,nivelSom:1,audioCtx:null,ganhoMestre:null,window:{AudioContext:AudioFalso}};const real=fs.readFileSync('src/govsim/view/app.js','utf8');vm.createContext(c);const a=real.indexOf('    function obterAudioContext()'),b=real.indexOf('    // =========================================================================',a);vm.runInContext(real.slice(a,b),c);
 for(const tipo of ['tilt','flip','swipe','alerta','vitoria','derrota'])c.tocarSom(tipo);
 assert.equal(criados,1);assert.ok(retomados>0);assert.ok(notas>=10);assert.equal(c.ganhoMestre.gain.value,.35);c.nivelSom=3;c.obterAudioContext();assert.equal(c.ganhoMestre.gain.value,.85);c.somAtivo=false;assert.equal(c.obterAudioContext(),null);
 console.log('APROVADO: áudio reutilizado, ganho mestre, seis sons e rejeição de resume tratada.');
}
