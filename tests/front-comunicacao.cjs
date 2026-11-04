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
 globalThis.teste={nomeCategoria,cenarioCategoria,iniciarNovaPartida,executarDecisao,carregarEstadoInicial,estado:()=>estadoAtual,processando:()=>processando};
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
 console.log('APROVADO: Diálogos do catálogo completos, sem truncamento, com até 20 palavras.');
}
{
 let criados=0,retomados=0,notas=0;const parametros=()=>({value:0,setValueAtTime(){},exponentialRampToValueAtTime(){}});
 class AudioFalso{constructor(){criados++;this.state='suspended';this.currentTime=0;this.destination={};}createGain(){return {gain:parametros(),connect(){}};}resume(){retomados++;return {catch(cb){cb(new Error('bloqueado'));}};}createOscillator(){notas++;return {frequency:parametros(),connect(){},start(){},stop(){}};}}
 const c={somAtivo:true,nivelSom:1,audioCtx:null,ganhoMestre:null,window:{AudioContext:AudioFalso}};const real=fs.readFileSync('src/govsim/view/app.js','utf8');vm.createContext(c);const a=real.indexOf('    function obterAudioContext()'),b=real.indexOf('    // =========================================================================',a);vm.runInContext(real.slice(a,b),c);
 for(const tipo of ['tilt','flip','swipe','alerta','vitoria','derrota'])c.tocarSom(tipo);
 assert.equal(criados,1);assert.ok(retomados>0);assert.ok(notas>=10);assert.equal(c.ganhoMestre.gain.value,.35);c.nivelSom=3;c.obterAudioContext();assert.equal(c.ganhoMestre.gain.value,.85);c.somAtivo=false;assert.equal(c.obterAudioContext(),null);
 console.log('APROVADO: áudio reutilizado, ganho mestre, seis sons e rejeição de resume tratada.');
}

{
 const cartas=JSON.parse(fs.readFileSync('src/govsim/engine/eventos.json','utf8'));
 const esperadas={FISCAL:'Fiscal',PRECO:'Preço',CAMBIO:'Câmbio',DESEMPREGO:'Desemprego',ENERGIA:'Energia',SUPERAQUECIMENTO:'Superaquecimento',EDUCACAO:'Educação',SAUDE:'Saúde',COMERCIO:'Comércio',FEDERALISMO:'Federalismo'};
 for(const c of cartas){assert.ok(esperadas[c.categoria],c.id);assert.equal(sandbox.teste.nomeCategoria(c.categoria),esperadas[c.categoria]);assert.ok(['saude','cambio','preco','energia','desemprego','fiscal','educacao'].includes(sandbox.teste.cenarioCategoria(c.categoria)));}
 console.log('APROVADO: categorias de todo o catálogo, acentos e cenários por categoria.');
}

// Renderização das funções reais, com DOM restrito aos IDs do HTML e canvas instrumentado.
{
 const html=fs.readFileSync('src/govsim/view/index.html','utf8'),ids=new Set([...html.matchAll(/id="([^"]+)"/g)].map(m=>m[1]));
 const nos=new Map(),erros=[];let desenhos=0;
 const ctxCanvas={clearRect(){desenhos++;},beginPath(){},moveTo(x,y){assert.ok(Number.isFinite(x)&&Number.isFinite(y));},lineTo(x,y){assert.ok(Number.isFinite(x)&&Number.isFinite(y));},stroke(){},setLineDash(){}};
 function no(id){if(nos.has(id))return nos.get(id);const classes=new Set();let texto='',markup='';const n={id,children:[],dataset:{},style:{setProperty(){}},width:600,height:180,focus(){this.focado=true;},showModal(){this.open=true;},close(){this.open=false;},setAttribute(k,v){this[k]=v;},removeAttribute(k){delete this[k];},appendChild(c){this.children.push(c);},getContext:()=>ctxCanvas,classList:{add(...cs){cs.forEach(c=>classes.add(c));},remove(...cs){cs.forEach(c=>classes.delete(c));},contains(c){return classes.has(c);},toggle(c,forca){if(forca===undefined)forca=!classes.has(c);if(forca)classes.add(c);else classes.delete(c);return forca;}}};Object.defineProperty(n,'textContent',{get:()=>texto,set(v){texto=String(v);n.children=[];}});Object.defineProperty(n,'innerHTML',{get:()=>markup,set(v){markup=String(v);n.children=[];}});nos.set(id,n);return n;}
 const seletores={'.endgame-analysis':no('analise'),'.conseq-memoria-details':no('memoria')};
 const ambiente={document:{addEventListener(){},body:{dataset:{}},getElementById(id){assert.ok(ids.has(id),'ID inexistente no HTML: '+id);return no(id);},createElement:tag=>no('criado-'+nos.size),querySelector(sel){assert.ok(seletores[sel],'Seletor não implementado: '+sel);return seletores[sel];}},window:{matchMedia:()=>({matches:true}),crypto:{randomUUID:()=> 'id'}},console:{error(...args){erros.push(args);},log(){},warn(){}},localStorage:{getItem:()=>null},performance};
 let codigo=fs.readFileSync('src/govsim/view/app.js','utf8'),fim=codigo.lastIndexOf('})();');codigo=codigo.slice(0,fim)+`globalThis.telas={definirGlossario,segmentarTermos,tratarCliqueGlossario,tratarTeclaGlossario,preview(e,d){estadoAtual=e;renderizarPreviaFaixa(d,document.getElementById("curtain-sub"));},final:exibirFimDeJogo,cronicas:preencherModalCronicas,posse(e){estadoAtual=e;renderizarCartaPersonagem(e.eventoAtual,false);},resultado(e){estadoAtual=e;exibirConsequenciaDoTurno(e);}};`+codigo.slice(fim);vm.createContext(ambiente);vm.runInContext(codigo,ambiente);
 const posse=JSON.parse(fs.readFileSync('src/govsim/engine/eventos.json','utf8')).find(c=>c.id==='EV_POSSE');
 let cenarios=0;
 for(const dificuldade of ['FACIL','MEDIO','DIFICIL'])for(const status of ['VITORIA','DERROTA_CAIXA','DERROTA_APROVACAO','DERROTA_AMBOS','DERROTA_HIPERINFLACAO','DERROTA_DEPRESSAO'])for(const tamanho of [0,2,{FACIL:10,MEDIO:12,DIFICIL:15}[dificuldade]])for(const semLicoes of [false,true]){
  const historico=Array.from({length:tamanho},(_,i)=>({numeroTurno:i+1,eventoId:'EV_POSSE',eventoNome:'Posse',decretoCodigo:'A',decretoTitulo:'Priorizar renda',decretoTipoPolitica:'Renda',caixaAntes:70,caixaDepois:60,aprovacaoAntes:50,aprovacaoDepois:55,pibAntes:2.5,pibDepois:3,inflacaoAntes:4.5,inflacaoDepois:5,deltaCaixa:-10,deltaAprovacao:5,deltaPib:.5,deltaInflacao:.5,sinteseDidatica:'Uma decisão. Uma consequência.',oQueMudou:'Mudança',porQueMudou:'Teoria',custoDecisao:'Custo',consequenciasFuturas:'Futuro',reacoesTurno:[],detalhesCalculo:['Conta'],efeitosAtrasadosAtivados:[]}));
  const indicadores={};for(const [k,v] of [['caixa',status.includes('CAIXA')||status.includes('AMBOS')?0:60],['aprovacao',status.includes('APROVACAO')||status.includes('AMBOS')?0:65],['pib',status.includes('DEPRESSAO')?-4:3],['inflacao',status.includes('HIPER')?26:5]])indicadores[k]={nome:k,valorInicial:50,valorAtual:v};
  const e={dificuldade,indicadores,historico,totalTurnos:{FACIL:10,MEDIO:12,DIFICIL:15}[dificuldade],turnoAtual:tamanho||1,partidaEncerrada:true,vitoria:status==='VITORIA',statusPartida:status,tituloFimDeJogo:'Resultado',motivoFimDeJogo:'Balanço',avaliacaoFinalEconomica:'Gestão Equilibrada:',epilogoMandato:'Seu governo ficou conhecido pelas decisões reais.',resultadoEleicao:'Resultado narrativo',licoesMandato:semLicoes?[]:['Turno 1 · Priorizar renda: financiamento.'],efeitosPendentes:[],reacoesAtuais:[],mostrarNumeros:dificuldade==='FACIL',herancaMandato:'Herança',dialogoApresentado:'O gabinete aguarda sua decisão.',eventoAtual:posse,manchete:'País acompanha o mandato'};
  if(e.vitoria&&tamanho===e.totalTurnos&&!semLicoes){for(const k of ['caixa','aprovacao'])e.indicadores[k].valorAtual=100;e.indicadores.pib.valorAtual=6;e.indicadores.inflacao.valorAtual=4;e.avaliacaoFinalEconomica='Desempenho de Excelência (Estadista)';}
  const antes=desenhos;assert.doesNotThrow(()=>ambiente.telas.final(e),dificuldade+'/'+status+'/'+tamanho);assert.ok(desenhos>antes,'Gráfico real deve ser desenhado');assert.ok(no('fim-classificacao').textContent);assert.equal(no('epilogo-mandato').textContent,e.epilogoMandato);assert.ok(no('fim-estadista-faltou').textContent);if(e.avaliacaoFinalEconomica.includes('(Estadista)')){assert.equal(no('fim-classificacao').textContent,'Estadista');assert.match(no('fim-estadista-faltou').textContent,/Todos os critérios/);}assert.ok(no('fim-aprendizados').children.length);assert.equal(no('grafico-resumo-texto').textContent.includes('Turno'),tamanho>0);
  assert.doesNotThrow(()=>ambiente.telas.preview(e,posse.decretoA));assert.equal(no("curtain-sub").children.some(n=>n.textContent===posse.decretoA.emPoucasPalavras),dificuldade!=="DIFICIL");
  assert.doesNotThrow(()=>ambiente.telas.cronicas(e));assert.doesNotThrow(()=>ambiente.telas.posse(e));if(tamanho)assert.doesNotThrow(()=>ambiente.telas.resultado(e));cenarios++;
 }
 const vocabulario=JSON.parse(fs.readFileSync('src/govsim/view/glossario.json','utf8'));ambiente.telas.definirGlossario(vocabulario);
 const partes=ambiente.telas.segmentarTermos('PIB, Selic e inflação; PIBão e prefiscal.');assert.equal(partes.filter(p=>p.id).length,3);assert.equal(partes.map(p=>p.texto).join(''),'PIB, Selic e inflação; PIBão e prefiscal.');
 const origem=no('ajuda-teste');origem.dataset.ajuda='pib';const evento=key=>({key,target:{closest:()=>origem},preventDefault(){}});
 ambiente.telas.tratarCliqueGlossario(evento());assert.equal(no('glossario-popover').open,true);assert.ok(no('glossario-definicao').textContent);ambiente.telas.tratarTeclaGlossario(evento('Escape'));assert.equal(no('glossario-popover').open,false);assert.equal(origem.focado,true);
 for(const tecla of ['Enter',' ']){ambiente.telas.tratarTeclaGlossario(evento(tecla));assert.equal(no('glossario-popover').open,true);ambiente.telas.tratarTeclaGlossario(evento('Escape'));}
 ambiente.telas.definirGlossario(vocabulario.concat([{id:'novo',termo:'Termo (novo)',definicao:'Explicação adicionada só ao arquivo.'}]));assert.equal(ambiente.telas.segmentarTermos('Termo (novo)').filter(p=>p.id==='novo').length,1);
 assert.deepEqual(erros,[]);console.log('APROVADO: glossário por dados, aliases, clique/toque, teclado, Esc e retorno do foco.');console.log('APROVADO: '+cenarios+' estados renderizam final/canvas/epílogo/critérios/lições, posse, resultado e Crônicas sem exceção ou console.error.');
}

{
 const antes=JSON.parse(fs.readFileSync('tests/fixtures/linguagem-antes.json','utf8')),atual=JSON.parse(fs.readFileSync('src/govsim/engine/eventos.json','utf8'));
 function numeros(o,p='',r={}){if(typeof o==='number')r[p]=o;else if(o&&typeof o==='object')for(const k of Object.keys(o))numeros(o[k],p+'/'+k,r);return r;}
 assert.deepEqual(numeros(antes),numeros(atual));for(let i=0;i<antes.length;i++)for(const k of ['id','categoria','tema','gatilho','decretoFavorito','tipoMandato'])assert.deepEqual(antes[i][k],atual[i][k]);
 console.log('APROVADO: linguagem preserva todos os números, IDs, categorias, temas e gatilhos do catálogo.');
}
