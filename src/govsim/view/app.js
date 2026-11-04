/**
 * GovSim — Experiência Fiel ao Estilo Reigns + Motor Econômico Java
 * Inclui:
 * - Ícones-recipiente no topo que se enchem verticalmente por dentro + Preview Dots
 * - Cortina superior de decisão dentro da carta ao inclinar para Esquerda/Direita
 * - Animação 3D de virar a carta do baralho (Card Flip)
 * - Arte vetorial geométrica bipartida (Low-Poly / Flat Reigns Art)
 * - Efeitos sonoros sintetizados via Web Audio API (100% offline)
 */
(function () {
    "use strict";

    function tempoVisual(nome,padrao){
        try{const valor=getComputedStyle(document.documentElement).getPropertyValue(nome).trim();return parseFloat(valor)*(valor.endsWith("ms")?1:1000)||padrao;}catch(ignorado){return padrao;}
    }
    const reduzirMovimento = window.matchMedia("(prefers-reduced-motion: reduce)");
    // ID por documento: abas duplicadas também recebem partidas independentes.
    const sessaoId = window.crypto.randomUUID ? window.crypto.randomUUID() : "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(/[xy]/g,function(c){const r=crypto.getRandomValues(new Uint8Array(1))[0]%16;return (c==="x"?r:(r&3|8)).toString(16);});
    function api(url, opcoes){opcoes=opcoes||{};opcoes.headers=Object.assign({},opcoes.headers,{"X-GovSim-Session":sessaoId});return fetch(url,opcoes);}
    let estadoAtual = null;
    let processando = false;
    let exibindoConsequencia = false;
    let somAtivo = true;
    try { somAtivo=localStorage.getItem("govsim-mute")!=="true"; } catch(ignorado) {}
    let ultimoAlertaCritico=false;
    let decisaoVisual=null;
    let retratoAnterior={evento:null,avatar:null,variante:0};
    let audioCtx = null;
    let ganhoMestre=null;
    let nivelSom=1;
    try{const salvo=Number(localStorage.getItem("govsim-volume"));if(salvo>=1&&salvo<=3)nivelSom=salvo;}catch(ignorado){}
    let ultimoLadoInclinado = 0;
    let timerSwipeDecisao = null;
    let animacaoCarta = null;
    let animandoCarta = false;
    let teclaDecreto = null;
    let timersFlash = {};

    // Controle de arraste (Drag / Swipe)
    let arrastando = false;
    let inicioX = 0;
    let inicioY = 0;
    let deltaXAtual = 0;
    const LIMIAR_SWIPE = 80;

    document.addEventListener("DOMContentLoaded", function () {
        atualizarBotaoSom();
        ["pointerdown","touchstart","keydown"].forEach(function(tipo){document.addEventListener(tipo,function(){obterAudioContext();},{passive:true});});
        document.querySelectorAll('input[name="desafio"]').forEach(function(radio){radio.addEventListener("change",function(){
            document.getElementById("inicio-dificuldade").value=radio.value;
            document.body.dataset.dificuldade=radio.value;
        });});
        document.body.dataset.dificuldade="FACIL";
        const tutorial=document.getElementById("tutorial-conteudo"),botaoTutorial=document.getElementById("btn-tutorial");
        botaoTutorial.addEventListener("click",function(){tutorial.classList.remove("hidden");tutorial.showModal();botaoTutorial.setAttribute("aria-expanded","true");});
        document.getElementById("tutorial-fechar").addEventListener("click",function(){tutorial.close();});
        tutorial.addEventListener("close",function(){tutorial.classList.add("hidden");botaoTutorial.setAttribute("aria-expanded","false");});
        configurarFocoModal();
        document.getElementById("inicio-jogar").addEventListener("click",function(){
            iniciarNovaPartida("normal",0);
        });
        document.querySelector(".reigns-viewport").inert=true;
        configurarBotoesEAtalhos();
        configurarArrasteCarta();
        carregarEstadoInicial();
    });

    // =========================================================================
    // SINTETIZADOR DE ÁUDIO PROCEDURAL (WEB AUDIO API - 100% OFFLINE)
    // =========================================================================
    function abrirMenu(demo){
        document.getElementById("modal-cronicas").classList.add("hidden");
        const tutorial=document.getElementById("tutorial-conteudo");if(tutorial.open)tutorial.close();
        document.getElementById("inicio").classList.remove("hidden");
        document.querySelector(".reigns-viewport").inert=true;
        const nivel=estadoAtual?estadoAtual.dificuldade:"FACIL";
        document.getElementById("inicio-dificuldade").value=nivel;
        const radio=document.querySelector('input[name="desafio"][value="'+nivel+'"]');if(radio)radio.checked=true;
        document.getElementById(demo?"btn-demo-curta":"inicio-jogar").focus();
    }
    function atualizarBotaoSom(){
        const botao=document.getElementById("btn-som");
        botao.innerHTML="<svg aria-hidden='true' viewBox='0 0 24 24' width='16' height='16'><path d='M3 9H7L12 5V19L7 15H3Z' fill='none' stroke='currentColor' stroke-width='1.7'/>"+(somAtivo?"<path d='M16 8L19 12 16 16' fill='none' stroke='currentColor' stroke-width='1.7'/>":"<path d='M16 8L22 16M22 8L16 16' stroke='currentColor' stroke-width='1.7'/>")+"</svg><span>"+(somAtivo?["","Baixo","Médio","Alto"][nivelSom]:"Mudo")+"</span>";
        botao.setAttribute("aria-label",somAtivo?"Som "+["","baixo","médio","alto"][nivelSom]+": mudar volume":"Som mudo: ativar");
        botao.setAttribute("aria-pressed",String(!somAtivo));
    }
    function configurarFocoModal(){
        const modal=document.getElementById("modal-cronicas");
        modal.addEventListener("keydown",function(e){
            if(e.key==="Escape"){modal.classList.add("hidden");document.getElementById("btn-abrir-cronicas").focus();e.stopPropagation();}
            if(e.key==="Tab"){
                const itens=Array.from(modal.querySelectorAll('button,summary,select,a[href],[tabindex="0"]')).filter(function(el){return el.getClientRects().length;});
                if(!itens.length)return;const primeiro=itens[0],ultimo=itens[itens.length-1];
                if(e.shiftKey&&document.activeElement===primeiro){ultimo.focus();e.preventDefault();}
                else if(!e.shiftKey&&document.activeElement===ultimo){primeiro.focus();e.preventDefault();}
            }
        });
    }
    function obterAudioContext() {
        try{
            if(!somAtivo)return null;
            if(!audioCtx){const Classe=window.AudioContext||window.webkitAudioContext;if(!Classe)return null;audioCtx=new Classe();ganhoMestre=audioCtx.createGain();ganhoMestre.connect(audioCtx.destination);}
            ganhoMestre.gain.value=[0,.35,.6,.85][nivelSom];
            if(audioCtx.state==="suspended"){const retomada=audioCtx.resume();if(retomada&&retomada.catch)retomada.catch(function(){});}
            return audioCtx;
        }catch(ignorado){return null;}
    }

    function tocarSom(tipo) {
        try {
            const ctx = obterAudioContext();
            if (!ctx) return;
            const agora = ctx.currentTime;

            if(tipo==="alerta"){
                [330,220].forEach(function(freq,i){const osc=ctx.createOscillator(),gain=ctx.createGain();osc.type="sine";osc.frequency.value=freq;gain.gain.setValueAtTime(0.20,agora+i*.12);gain.gain.exponentialRampToValueAtTime(.001,agora+i*.12+.18);osc.connect(gain);gain.connect(ganhoMestre);osc.start(agora+i*.12);osc.stop(agora+i*.12+.19);});
            } else if (tipo === "tilt") {
                const osc = ctx.createOscillator();
                const gain = ctx.createGain();
                osc.type = "triangle";
                osc.frequency.setValueAtTime(240, agora);
                osc.frequency.exponentialRampToValueAtTime(310, agora + 0.045);
                gain.gain.setValueAtTime(0.12, agora);
                gain.gain.exponentialRampToValueAtTime(0.001, agora + 0.045);
                osc.connect(gain);
                gain.connect(ganhoMestre);
                osc.start(agora);
                osc.stop(agora + 0.05);
            } else if (tipo === "swipe") {
                const osc = ctx.createOscillator();
                const gain = ctx.createGain();
                osc.type = "sine";
                osc.frequency.setValueAtTime(190, agora);
                osc.frequency.exponentialRampToValueAtTime(95, agora + 0.14);
                gain.gain.setValueAtTime(0.20, agora);
                gain.gain.exponentialRampToValueAtTime(0.001, agora + 0.14);
                osc.connect(gain);
                gain.connect(ganhoMestre);
                osc.start(agora);
                osc.stop(agora + 0.15);
            } else if (tipo === "flip") {
                const osc = ctx.createOscillator();
                const gain = ctx.createGain();
                osc.type = "triangle";
                osc.frequency.setValueAtTime(320, agora);
                osc.frequency.exponentialRampToValueAtTime(480, agora + 0.09);
                gain.gain.setValueAtTime(0.15, agora);
                gain.gain.exponentialRampToValueAtTime(0.001, agora + 0.09);
                osc.connect(gain);
                gain.connect(ganhoMestre);
                osc.start(agora);
                osc.stop(agora + 0.1);
            } else if (tipo === "vitoria") {
                [261.6, 329.6, 392.0, 523.2].forEach(function (freq, idx) {
                    const osc = ctx.createOscillator();
                    const gain = ctx.createGain();
                    osc.type = "triangle";
                    osc.frequency.setValueAtTime(freq, agora + idx * 0.09);
                    gain.gain.setValueAtTime(0.16, agora + idx * 0.09);
                    gain.gain.exponentialRampToValueAtTime(0.001, agora + idx * 0.09 + 0.35);
                    osc.connect(gain);
                    gain.connect(ganhoMestre);
                    osc.start(agora + idx * 0.09);
                    osc.stop(agora + idx * 0.09 + 0.36);
                });
            } else if (tipo === "derrota") {
                [220.0, 196.0, 164.8, 130.8].forEach(function (freq, idx) {
                    const osc = ctx.createOscillator();
                    const gain = ctx.createGain();
                    osc.type = "sawtooth";
                    osc.frequency.setValueAtTime(freq, agora + idx * 0.12);
                    gain.gain.setValueAtTime(0.15, agora + idx * 0.12);
                    gain.gain.exponentialRampToValueAtTime(0.001, agora + idx * 0.12 + 0.4);
                    osc.connect(gain);
                    gain.connect(ganhoMestre);
                    osc.start(agora + idx * 0.12);
                    osc.stop(agora + idx * 0.12 + 0.42);
                });
            }
        } catch (ignored) {}
    }

    // =========================================================================
    // EVENTOS DE INTERFACE E ARRASTE (SWIPE)
    // =========================================================================
    function configurarBotoesEAtalhos() {
        const btnA = document.getElementById("btn-assinar-a");
        const btnB = document.getElementById("btn-assinar-b");
        const btnProxima = document.getElementById("btn-proxima-carta");
        const btnMenu = document.getElementById("btn-menu");
        const btnFimNormal = document.getElementById("btn-fim-nova-normal");
        const btnFimDemo = document.getElementById("btn-fim-nova-demo");
        const btnDemoCurta = document.getElementById("btn-demo-curta");
        const btnDemo10 = document.getElementById("btn-demo-10");
        const btnAbrirCronicas = document.getElementById("btn-abrir-cronicas");
        const btnFecharCronicas = document.getElementById("btn-fechar-cronicas");
        const modalCronicas = document.getElementById("modal-cronicas");
        const btnSom = document.getElementById("btn-som");

        if (btnSom) {
            btnSom.addEventListener("click", function () {
                if(!somAtivo){somAtivo=true;nivelSom=1;}else if(nivelSom<3){nivelSom++;}else{somAtivo=false;if(ganhoMestre)ganhoMestre.gain.value=0;}
                try{localStorage.setItem("govsim-mute",String(!somAtivo));localStorage.setItem("govsim-volume",String(nivelSom));}catch(ignorado){}
                atualizarBotaoSom();if(somAtivo)tocarSom("tilt");
            });
        }

        if (btnA) {
            btnA.addEventListener("mouseenter",function(){simularInclinacao(-42);});btnA.addEventListener("mouseleave",resetarInclinacaoCarta);
            btnA.addEventListener("focus",function(){simularInclinacao(-42);});
            btnA.addEventListener("blur",resetarInclinacaoCarta);
            btnA.addEventListener("click", function () { executarDecisao("A"); });
        }

        if (btnB) {
            btnB.addEventListener("mouseenter",function(){simularInclinacao(42);});btnB.addEventListener("mouseleave",resetarInclinacaoCarta);
            btnB.addEventListener("focus",function(){simularInclinacao(42);});
            btnB.addEventListener("blur",resetarInclinacaoCarta);
            btnB.addEventListener("click", function () { executarDecisao("B"); });
        }

        if (btnProxima) {
            btnProxima.addEventListener("click", function () {
                fecharConsequenciaEVirarProxima();
            });
        }

        if (btnMenu) btnMenu.addEventListener("click", function () { abrirMenu(false); });
        if (btnFimNormal) btnFimNormal.addEventListener("click", function () { abrirMenu(false); });
        if (btnFimDemo) btnFimDemo.addEventListener("click", function () { abrirMenu(true); });
        if (btnDemoCurta) btnDemoCurta.addEventListener("click", function () { iniciarNovaPartida("demo", 4); });
        if (btnDemo10) btnDemo10.addEventListener("click", function () { iniciarNovaPartida("demo", 10); });

        function abrirModalCronicas() {
            if (!modalCronicas) return;
            modalCronicas.classList.remove("hidden");
            document.getElementById("btn-fechar-cronicas").focus();
            if (estadoAtual) desenharGrafico("grafico-evolucao", estadoAtual);
        }

        if (btnAbrirCronicas) btnAbrirCronicas.addEventListener("click", abrirModalCronicas);

        if (btnFecharCronicas && modalCronicas) {
            btnFecharCronicas.addEventListener("click", function () {
                modalCronicas.classList.add("hidden");
                document.getElementById("btn-abrir-cronicas").focus();
            });
            modalCronicas.addEventListener("click", function (ev) {
                if (ev.target === modalCronicas) modalCronicas.classList.add("hidden");
            });
        }

        document.addEventListener("keydown", function (e) {
            if(e.ctrlKey && e.key.toLowerCase()==="d"){e.preventDefault();abrirMenu(true);return;}
            if(e.ctrlKey||e.altKey||e.metaKey)return;
            if(!document.getElementById("inicio").classList.contains("hidden") || /SELECT|INPUT/.test(e.target.tagName))return;
            if (modalCronicas && !modalCronicas.classList.contains("hidden")) {
                if (e.key === "Escape") modalCronicas.classList.add("hidden");
                return;
            }
            if(e.key==="Escape"){teclaDecreto=null;arrastando=false;document.getElementById("carta-ativa").classList.remove("dragging");resetarInclinacaoCarta();return;}
            if (processando || animandoCarta) return;

            if (exibindoConsequencia) {
                if (e.key === "Enter" || e.key === " ") {
                    e.preventDefault();
                    fecharConsequenciaEVirarProxima();
                }
                return;
            }

            if (estadoAtual && !estadoAtual.partidaEncerrada) {
                if (e.key === "ArrowLeft" || e.key === "a" || e.key === "A") {
                    e.preventDefault();
                    if(!e.repeat){teclaDecreto="A";simularInclinacao(-42);}
                } else if (e.key === "ArrowRight" || e.key === "b" || e.key === "B" || e.key === "d" || e.key === "D") {
                    e.preventDefault();
                    if(!e.repeat){teclaDecreto="B";simularInclinacao(42);}
                }
            }
        });
        document.addEventListener("keyup",function(e){
            const codigo=/^(ArrowLeft|a|A)$/.test(e.key)?"A":/^(ArrowRight|b|B|d|D)$/.test(e.key)?"B":null;
            if(codigo && codigo===teclaDecreto){teclaDecreto=null;executarDecisao(codigo);}
        });
        window.addEventListener("blur",function(){teclaDecreto=null;arrastando=false;document.getElementById("carta-ativa").classList.remove("dragging");resetarInclinacaoCarta();});
    }

    function configurarArrasteCarta() {
        const carta=document.getElementById("carta-ativa");let ponteiro=null;
        const palco=carta.closest(".card-arena");
        palco.addEventListener("pointermove",function(e){
            if(e.pointerType!=="mouse"||arrastando||processando||animandoCarta||exibindoConsequencia||!estadoAtual||estadoAtual.partidaEncerrada)return;
            const caixa=palco.getBoundingClientRect();
            const posicao=Math.max(-1,Math.min(1,(e.clientX-caixa.left-caixa.width/2)/(caixa.width/2)));
            if(Math.abs(posicao)<.12){resetarInclinacaoCarta();return;}
            simularInclinacao(posicao*42);
        });
        palco.addEventListener("pointerleave",function(){if(!arrastando)resetarInclinacaoCarta();});
        function cancelar(){ponteiro=null;arrastando=false;deltaXAtual=0;carta.classList.remove("dragging");resetarInclinacaoCarta();}
        carta.addEventListener("pointerdown",function(e){
            if(e.button!==0||processando||animandoCarta||exibindoConsequencia||!estadoAtual||estadoAtual.partidaEncerrada)return;
            ponteiro=e.pointerId;inicioX=e.clientX;deltaXAtual=0;arrastando=true;carta.setPointerCapture(ponteiro);carta.classList.add("dragging");
        });
        carta.addEventListener("pointermove",function(e){
            if(e.pointerId!==ponteiro)return;deltaXAtual=e.clientX-inicioX;
            const rot=reduzirMovimento.matches?0:Math.max(-15,Math.min(15,deltaXAtual*.12));
            carta.style.transform="translateX("+deltaXAtual+"px) rotate("+rot+"deg)";atualizarCortinaEDots(deltaXAtual);
        });
        carta.addEventListener("pointerup",function(e){
            if(e.pointerId!==ponteiro)return;
            const dx=deltaXAtual,limite=carta.offsetWidth*.35;ponteiro=null;arrastando=false;carta.classList.remove("dragging");
            if(carta.hasPointerCapture(e.pointerId))carta.releasePointerCapture(e.pointerId);
            if(Math.abs(dx)>=limite)executarDecisao(dx<0?"A":"B");else resetarInclinacaoCarta();deltaXAtual=0;
        });
        carta.addEventListener("pointercancel",cancelar);carta.addEventListener("lostpointercapture",function(){if(ponteiro!==null)cancelar();});window.addEventListener("resize",cancelar);
    }

    function simularInclinacao(dx) {
        if (processando || animandoCarta || exibindoConsequencia || arrastando) return;
        const carta = document.getElementById("carta-ativa");
        if (carta) {
            carta.style.transform = "translateX(" + dx + "px) rotate(" + (reduzirMovimento.matches?0:Math.max(-10,Math.min(10,dx*10/42))) + "deg)";
        }
        atualizarCortinaEDots(dx * 2);
    }

    function resetarInclinacaoCarta() {
        const carta = document.getElementById("carta-ativa");
        if (carta && !arrastando) carta.style.transform = "";
        if(carta){carta.style.setProperty("--reveal-a",0);carta.style.setProperty("--reveal-b",0);}
        ultimoLadoInclinado = 0;

        const curtain = document.getElementById("choice-curtain");
        if (curtain) {curtain.classList.remove("visible");curtain.setAttribute("aria-hidden","true");}

        document.getElementById("btn-assinar-a").classList.remove("active-wing");
        document.getElementById("btn-assinar-b").classList.remove("active-wing");

        ["caixa", "aprovacao", "pib", "inflacao"].forEach(function (k) {
            const dot = document.getElementById("dot-" + k);
            if (dot) {dot.className = "reigns-dot";dot.textContent="";dot.removeAttribute("aria-label");}
        });
    }

    function atualizarCortinaEDots(dx) {
        const retrato=document.getElementById("carta-ativa");
        retrato.style.setProperty("--reveal-a",Math.min(1,Math.max(0,-dx)/110));
        retrato.style.setProperty("--reveal-b",Math.min(1,Math.max(0,dx)/110));
        if (!estadoAtual || !estadoAtual.eventoAtual) return;
        const curtain = document.getElementById("choice-curtain");
        const curtainText = document.getElementById("curtain-text");
        const curtainSub = document.getElementById("curtain-sub");
        const btnA = document.getElementById("btn-assinar-a");
        const btnB = document.getElementById("btn-assinar-b");

        if (dx < -16) {
            if (ultimoLadoInclinado !== -1) {
                ultimoLadoInclinado = -1;
                tocarSom("tilt");
            }
            const decA = estadoAtual.eventoAtual.decretoA;
            curtainText.textContent = "\u25C4 " + decA.titulo;
            renderizarPreviaFaixa(decA,curtainSub);
            curtain.classList.add("visible");curtain.setAttribute("aria-hidden","false");
            btnA.classList.add("active-wing");
            btnB.classList.remove("active-wing");
            acenderDotsReigns(decA);
        } else if (dx > 16) {
            if (ultimoLadoInclinado !== 1) {
                ultimoLadoInclinado = 1;
                tocarSom("tilt");
            }
            const decB = estadoAtual.eventoAtual.decretoB;
            curtainText.textContent = decB.titulo + " \u25BA";
            renderizarPreviaFaixa(decB,curtainSub);
            curtain.classList.add("visible");curtain.setAttribute("aria-hidden","false");
            btnB.classList.add("active-wing");
            btnA.classList.remove("active-wing");
            acenderDotsReigns(decB);
        } else {
            ultimoLadoInclinado = 0;
            curtain.classList.remove("visible");curtain.setAttribute("aria-hidden","true");
            btnA.classList.remove("active-wing");
            btnB.classList.remove("active-wing");
            ["caixa", "aprovacao", "pib", "inflacao"].forEach(function (k) {
                const dot=document.getElementById("dot-"+k);dot.className="reigns-dot";dot.textContent="";dot.removeAttribute("aria-label");
            });
        }
    }

    function renderizarPreviaFaixa(decreto,alvo) {
        alvo.textContent="";
        if(!estadoAtual.mostrarNumeros)return;
        [["caixa","Caixa","impactoCaixa"],["aprovacao","Aprovação","impactoAprovacao"],["pib","PIB","impactoPib"],["inflacao","Inflação","impactoInflacao"]].forEach(function(info){
            const chip=document.createElement("span");chip.className="effect-chip";
            chip.innerHTML=iconeIndicador(info[0])+"<span>"+info[1]+" "+formatarSinal(decreto[info[2]])+"</span>";alvo.appendChild(chip);
        });
    }

    function acenderDotsReigns(decreto) {
        const dificil=estadoAtual.dificuldade==="DIFICIL";
        [["caixa","impactoCaixa",12],["aprovacao","impactoAprovacao",8],["pib","impactoPib",1.2],["inflacao","impactoInflacao",.9]].forEach(function(info){
            const el=document.getElementById("dot-"+info[0]);el.textContent="";el.removeAttribute("title");
            let tamanho=0;
            if(!dificil && decreto){
                if(decreto.direcoes){const intensidade=((decreto.direcoes[info[0]]||"").match(/[↑↓]/g)||[]).length;tamanho=Math.min(2,intensidade);}
                else {const valor=Math.abs(decreto[info[1]]||0);tamanho=valor<.1?0:valor>=info[2]?2:1;}
            }
            el.className="reigns-dot"+(tamanho===2?" dot-large":tamanho===1?" dot-small":"");
            el.setAttribute("aria-label",tamanho?(tamanho===2?"Impacto intenso":"Impacto moderado"):"");
        });
    }

    // =========================================================================
    // COMUNICAÇÃO COM O MOTOR ECONÔMICO JAVA
    // =========================================================================
    function mostrarErro(mensagem) {
        const aviso=document.getElementById("erro-api");
        aviso.textContent=mensagem;
        aviso.classList.remove("hidden");
    }

    function limparErro() {
        document.getElementById("erro-api").classList.add("hidden");
    }

    async function lerResposta(resposta) {
        let dados;
        try { dados=await resposta.json(); }
        catch (causa) { const erro=new Error("O servidor retornou uma resposta inválida."); erro.status=resposta.status; throw erro; }
        if(!resposta.ok || !dados || dados.erro) {
            const erro=new Error(dados && dados.erro ? dados.erro : "Falha na comunicação com o servidor.");
            erro.status=resposta.status;
            throw erro;
        }
        if(!dados.indicadores || typeof dados.turnoAtual!=="number")throw new Error("Estado recebido inválido.");
        return dados;
    }

    function carregarEstadoInicial() {
        return api("/api/estado").then(lerResposta).then(function(dados){
            if(!processando){estadoAtual=dados;renderizarTudo(dados,true);}
        }).catch(function(erro){mostrarErro(erro.message || "Não foi possível carregar a partida.");});
    }

    function iniciarNovaPartida(modo, turnos) {
        if(processando)return;
        if(timerSwipeDecisao){clearTimeout(timerSwipeDecisao);timerSwipeDecisao=null;}
        processando=true;
        document.getElementById("inicio-jogar").disabled=true;
        return api("/api/iniciar",{
            method:"POST",
            headers:{"Content-Type":"application/json"},
            body:JSON.stringify({modo:modo,dificuldade:document.getElementById("inicio-dificuldade").value,turnos:modo==="demo"?(turnos||10):0})
        }).then(lerResposta).then(function(dados){
            estadoAtual=dados;
            retratoAnterior.evento=null;
            exibindoConsequencia=false;
            limparErro();
            document.getElementById("inicio").classList.add("hidden");
            document.querySelector(".reigns-viewport").inert=false;
            tocarSom("flip");
            renderizarTudo(dados,true);
            document.getElementById("btn-menu").focus();
        }).catch(function(erro){
            mostrarErro(erro.message || "Não foi possível iniciar a partida. O estado anterior foi preservado.");
        }).finally(function(){processando=false;document.getElementById("inicio-jogar").disabled=false;});
    }

    async function ressincronizarPartida() {
        const dados=await api("/api/estado").then(lerResposta);
        estadoAtual=dados;
        exibindoConsequencia=false;
        renderizarTudo(dados,false);
    }

    function executarDecisao(codigo) {
        if(processando || animandoCarta || exibindoConsequencia || !estadoAtual || estadoAtual.partidaEncerrada)return;
        processando=true;
        tocarSom("swipe");
        const turnoEnvio=estadoAtual.turnoAtual;
        const cartaAntes=estadoAtual.eventoAtual||{},decretoAntes=cartaAntes[codigo==="A"?"decretoA":"decretoB"]||{};
        const descricaoVisual={turno:turnoEnvio,descricao:decretoAntes.descricao,nota:cartaAntes.contextualizacao};
        const carta=document.getElementById("carta-ativa");
        resetarInclinacaoCarta();
        if(carta)carta.classList.add(codigo==="A"?"fly-left":"fly-right");
        timerSwipeDecisao=setTimeout(function(){
            timerSwipeDecisao=null;
            return api("/api/decidir",{
                method:"POST",headers:{"Content-Type":"application/json"},
                body:JSON.stringify({decreto:codigo,turno:turnoEnvio})
            }).then(lerResposta).then(function(dados){
                decisaoVisual=descricaoVisual;
                estadoAtual=dados;
                limparErro();
                atualizarPilaresERodape(dados,true);
                preencherModalCronicas(dados);
                exibirConsequenciaDoTurno(dados);
            }).catch(async function(erro){
                if(carta)carta.classList.remove("fly-left","fly-right");
                mostrarErro(erro.message || "A conexão foi interrompida durante a decisão.");
                // Em 409 ou resultado ambíguo, consultar o servidor; nunca reenviar a decisão.
                if(erro.status===409 || !erro.status || erro.status>=500 || erro.status===200){
                    try { await ressincronizarPartida(); }
                    catch(falha){mostrarErro("Não foi possível confirmar o resultado da decisão. " + (falha.message || "Verifique a conexão.")+" O último estado conhecido foi preservado.");}
                }
            }).finally(function(){processando=false;});
        },tempoVisual("--duration-normal",300));
    }

    const contagens={};
    function animarNumero(el,valor,prefixo,sufixo,animar){
        if(contagens[el.id])cancelAnimationFrame(contagens[el.id]);
        const antes=el.dataset.valor===undefined?valor:Number(el.dataset.valor);el.dataset.valor=valor;
        if(!animar||reduzirMovimento.matches){el.textContent=prefixo+formatarNum(valor)+sufixo;return;}
        const inicio=performance.now(),duracao=tempoVisual("--duration-slow",480);
        function passo(agora){const t=Math.min(1,(agora-inicio)/duracao);el.textContent=prefixo+formatarNum(antes+(valor-antes)*(1-Math.pow(1-t,3)))+sufixo;if(t<1)contagens[el.id]=requestAnimationFrame(passo);}
        contagens[el.id]=requestAnimationFrame(passo);
    }
    function renderizarComplementos(estado,animar){
        const timeline=document.getElementById("linha-tempo");timeline.textContent="";
        const fila=estado.efeitosPendentes||[];
        timeline.classList.remove("hidden");
        [["caixa","Caixa","deltaCaixa"],["aprovacao","Aprovação","deltaAprovacao"],["pib","PIB","deltaPib"],["inflacao","Inflação","deltaInflacao"]].forEach(function(info){
            const grupos={};
            fila.forEach(function(e){if(e[info[2]]){const t=e.turnosRestantes;grupos[t]=(grupos[t]||0)+e[info[2]];}});
            const turnos=Object.keys(grupos).sort(function(a,b){return Number(a)-Number(b);});
            const quadrado=document.createElement("div");quadrado.className="timeline-square"+(!turnos.length?" empty":"");
            const descricao=turnos.map(function(t){return "Turno "+(estado.turnoAtual+Number(t)-1)+" "+(grupos[t]===0?"↕":grupos[t]>0?"↑":"↓");}).join(" · ");
            quadrado.innerHTML=iconeIndicador(info[0])+"<span>"+(descricao||"—")+"</span>";
            quadrado.setAttribute("aria-label",info[1]+": "+(descricao||"sem efeitos pendentes"));
            quadrado.title=info[1]+": "+(descricao||"sem efeitos pendentes");timeline.appendChild(quadrado);
        });
        let alertas=[];
        ["caixa","aprovacao","pib","inflacao"].forEach(function(k){
            const ind=estado.indicadores[k],pilar=document.getElementById("hud-"+k);
            // Reserva <20; PIB negativo e inflação alta têm sentido econômico inverso.
            const reserva=k==="caixa"||k==="aprovacao";
            const risco=reserva?ind.valorAtual<20:k==="pib"?ind.valorAtual<0:ind.valorAtual>=20;
            const atencao=reserva?ind.valorAtual<45:k==="pib"?ind.valorAtual<1:ind.valorAtual>6;
            pilar.dataset.faixa=risco?"perigo":atencao?"atencao":"seguro";
            pilar.classList.toggle("reserva-baixa",reserva&&ind.valorAtual<25);
            pilar.classList.toggle("em-risco",risco);
            if(risco)alertas.push(ind.nome+" em zona de risco");
            let aviso=pilar.querySelector(".aviso-risco");if(!aviso){aviso=document.createElement("span");aviso.className="aviso-risco";pilar.appendChild(aviso);}aviso.textContent=risco?"⚠":atencao?"◇":"";
            pilar.setAttribute("aria-label",ind.nome+": "+(risco?"perigo":atencao?"atenção":"sem alerta"));

        });
        const critico=estado.indicadores.caixa.valorAtual<25||estado.indicadores.aprovacao.valorAtual<25;
        document.body.classList.toggle("risco-ativo",critico);
        if(critico&&!ultimoAlertaCritico&&animar)tocarSom("alerta");ultimoAlertaCritico=critico;
        document.getElementById("alerta-acessivel").textContent=alertas.join(". ");
        if(animar&&!reduzirMovimento.matches){const mesa=document.getElementById("mesa-cartas");mesa.classList.remove("novo-turno");void mesa.offsetWidth;mesa.classList.add("novo-turno");}
    }

    function renderizarTudo(estado, animarFlip) {
        atualizarPilaresERodape(estado, false);
        preencherModalCronicas(estado);
        document.getElementById("carta-consequencia").classList.add("hidden");

        if (estado.partidaEncerrada) {
            document.getElementById("mesa-cartas").classList.add("hidden");
            exibirFimDeJogo(estado);
        } else {
            document.getElementById("tela-fim-de-jogo").classList.add("hidden");
            document.getElementById("mesa-cartas").classList.remove("hidden");
            renderizarCartaPersonagem(estado.eventoAtual, animarFlip);
        }
    }

    function atualizarPilaresERodape(estado, animarFlash) {
        document.body.dataset.dificuldade=estado.dificuldade;
        document.body.dataset.numeros=String(estado.mostrarNumeros);
        document.getElementById("badge-dificuldade").textContent={FACIL:"Fácil",MEDIO:"Médio",DIFICIL:"Difícil"}[estado.dificuldade]||estado.dificuldade;
        renderizarComplementos(estado,animarFlash);
        const ano = 2025 + estado.turnoAtual;
        document.getElementById("ano-mandato").textContent = "Ano " + ano;
        document.getElementById("turno-atual").textContent = estado.turnoAtual;
        document.getElementById("turno-total").textContent = estado.totalTurnos;
        document.getElementById("valor-juros").textContent = formatarNum(estado.taxaJuros) + "% a.a.";

        const pendentes = (estado.efeitosPendentes || []).length;
        const contagem=document.getElementById("badge-efeitos-count");
        contagem.classList.toggle("hidden",pendentes===0);
        contagem.textContent=pendentes?"("+pendentes+" "+(pendentes===1?"efeito":"efeitos")+")":"";

        const badgeModo = document.getElementById("badge-modo");
        badgeModo.textContent=estado.modoDemonstracao?"Demo":"";
        badgeModo.classList.toggle("demo-active",estado.modoDemonstracao);

        atualizarPilarVessel("caixa", estado.indicadores.caixa, "", false, animarFlash);
        atualizarPilarVessel("aprovacao", estado.indicadores.aprovacao, "%", false, animarFlash);
        atualizarPilarVessel("pib", estado.indicadores.pib, "%", false, animarFlash);
        atualizarPilarVessel("inflacao", estado.indicadores.inflacao, "%", true, animarFlash);
    }

    function atualizarPilarVessel(chave, ind, sufixo, inverterCor, animarFlash) {
        const prefixo = (chave === "pib" && ind.valorAtual > 0) ? "+" : "";
        const numero=document.getElementById("valor-"+chave);
        animarNumero(numero,ind.valorAtual,prefixo,sufixo,animarFlash);

        const vesselFill = document.getElementById("vessel-fill-" + chave);
        const topoRecorte = Math.max(0, Math.min(100, 100 - ind.percentualBarra));
        vesselFill.style.clipPath = "inset(" + topoRecorte.toFixed(1) + "% 0 0 0)";

        const deltaEl = document.getElementById("float-" + chave);
        const d = ind.variacaoUltimoTurno || 0;

        if ( timersFlash[chave] ) {
            clearTimeout(timersFlash[chave]);
            timersFlash[chave] = null;
        }

        if (animarFlash && Math.abs(d) >= 0.05) {
            let bom = d > 0;
            if (inverterCor) bom = d < 0;

            vesselFill.classList.remove("flash-up", "flash-down");
            vesselFill.classList.add(bom ? "flash-up" : "flash-down");
            deltaEl.textContent = estadoAtual.mostrarNumeros ? formatarSinal(d) + sufixo : (d > 0 ? "↑" : "↓");
            deltaEl.className = "pillar-delta " + (bom ? "show-up" : "show-down");

            timersFlash[chave] = setTimeout(function () {
                vesselFill.classList.remove("flash-up", "flash-down");
                deltaEl.className = "pillar-delta";
            }, 2200);
        } else {
            vesselFill.classList.remove("flash-up", "flash-down");
            deltaEl.className = "pillar-delta";
        }
    }

    // Rótulos curtos das cartas históricas: o catálogo original permanece intacto.
    function tituloVisual(id,codigo,original){
        const titulos={
          "Subir Juros Selic (+2,5 p.p.)": "Subir a Selic",
          "Subsidiar Importados com Caixa": "Subsidiar importações",
          "Desonerar Cesta Básica e Safra": "Desonerar a cesta",
          "Negar Subsídio e Subir Juros": "Elevar juros",
          "Tesouro Paga a Conta de Luz": "Subsidiar a energia",
          "Cobrar Bandeira Vermelha": "Cobrar bandeira vermelha",
          "Lançar Obras Públicas (PAC)": "Financiar obras públicas",
          "Cortar Juros (-2,0 p.p.) Privado": "Cortar a Selic",
          "Cortar Gastos e Fazer Superávit": "Cortar gastos",
          "Manter Gastos e Emitir Dívida": "Emitir dívida",
          "Subsidiar Diesel e Fertilizantes": "Subsidiar diesel e fertilizantes",
          "Repassar Alta nos Postos": "Repassar custos externos",
          "Elevar Impostos e Contingenciar": "Elevar impostos",
          "Reduzir Alíquotas e Estimular PIB": "Reduzir alíquotas",
          "Poupar Caixa e Subir Juros (+1,5)": "Poupar e subir juros",
          "Acelerar Gastos e Cortar Juros": "Gastar e cortar juros",
          "Financiar e Desonerar Exportação": "Apoiar exportações",
          "Sobretaxar Produtos Importados": "Taxar importações",
          "Investir no Ensino e Ciência": "Investir em educação",
          "Contingenciar Verba da Educação": "Cortar verbas educacionais",
          "Socorrer Cofres dos Estados": "Socorrer os estados",
          "Negar Resgate e Exigir Ajuste": "Exigir ajuste estadual",
          "Conceder Reajuste Salarial": "Reajustar salários",
          "Congelar Salários e Cortar Ponto": "Congelar salários",
          "Renegociar Dívidas e Cortar Juros": "Renegociar dívidas",
          "Manter Rigor Bancário (+1,0 Juros)": "Restringir o crédito"
};
        const normalizar=function(t){return t.replace(/\([^)]*\)/g,"").replace(/\s+/g," ").trim();};
        const chave=Object.keys(titulos).find(function(t){return normalizar(t)===normalizar(original);});
        return chave?titulos[chave]:original;
    }
    function iconeIndicador(chave){
        const caminhos={caixa:"M3 9L12 4 21 9ZM5 11V19M10 11V19M14 11V19M19 11V19M3 21H21",aprovacao:"M12 21L3 12V7L7 4 12 8 17 4 21 7V12Z",pib:"M4 21V13H8V21M10 21V8H14V21M16 21V3H20V21",inflacao:"M18 5A9 9 0 1 0 20 16M18 2V7H13M9 15L15 9"};
        return "<svg aria-hidden='true' viewBox='0 0 24 24' class='indicator-icon'><path d='"+caminhos[chave]+"' fill='none' stroke='currentColor' stroke-width='1.7' stroke-linecap='square' stroke-linejoin='miter'/></svg>";
    }
    function definirViradaAtiva(ativa) {
        animandoCarta=ativa;
        document.getElementById("carta-ativa").setAttribute("aria-busy",String(ativa));
        ["btn-assinar-a","btn-assinar-b"].forEach(function(id){document.getElementById(id).disabled=ativa;});
    }

    function cancelarViradaCarta() {
        const anterior=animacaoCarta;
        animacaoCarta=null;
        if(anterior)anterior.cancel();
        document.getElementById("card-inner-3d").classList.remove("revelando");
        definirViradaAtiva(false);
    }

    function revelarProximaCarta(inner3d) {
        if(!inner3d.animate)return;
        // O primeiro quadro já é o verso: não inicia um giro para depois revertê-lo.
        const reduzido=reduzirMovimento.matches;
        const quadros=reduzido?[{opacity:0},{opacity:1}]:[{transform:"rotateY(180deg)"},{transform:"rotateY(0deg)"}];
        definirViradaAtiva(true);
        inner3d.classList.add("revelando");
        const animacao=inner3d.animate(quadros,{
            duration:tempoVisual(reduzido?"--duration-card-fade":"--duration-card-flip",reduzido?140:520),
            easing:"cubic-bezier(.22,1,.36,1)"
        });
        animacaoCarta=animacao;
        function concluir(){
            if(animacaoCarta!==animacao)return;
            animacaoCarta=null;
            inner3d.classList.remove("revelando");
            definirViradaAtiva(false);
        }
        animacao.onfinish=concluir;
        animacao.oncancel=concluir;
        tocarSom("flip");
    }

    function renderizarCartaPersonagem(evento, animarFlip) {
        if (!evento) return;
        const nome=(evento.nome+" "+evento.categoria).normalize("NFD").replace(/[\u0300-\u036f]/g,"").toLowerCase();
        document.body.dataset.cenario=/saude|sanitari|pandemia/.test(nome)?"saude":/energia|energet|apaga|reservatorio/.test(nome)?"energia":/cambio|cambial|dolar|guerra|comercio exterior/.test(nome)?"cambio":/educa|ensino|ciencia|requalifica/.test(nome)?"educacao":/desemprego|fechamento|demiss/.test(nome)?"desemprego":/trabalho|greve|informal|automacao/.test(nome)?"trabalho":/preco|alimento|seca|credito|consumo|imobili|imoveis|portos/.test(nome)?"preco":"fiscal";
        const carta = document.getElementById("carta-ativa");
        const inner3d = document.getElementById("card-inner-3d");

        carta.classList.remove("fly-left", "fly-right");
        carta.style.transform = "";

        cancelarViradaCarta();

        document.getElementById("evento-categoria").textContent = ({PRECO:"Preço",CAMBIO:"Câmbio",EDUCACAO:"Educação",ENERGIA:"Energia",FISCAL:"Fiscal",SAUDE:"Saúde",DESEMPREGO:"Desemprego",TRABALHO:"Trabalho"})[evento.categoria]||evento.categoria;
        document.getElementById("evento-nome").textContent = evento.nome;
        const fala=(evento.dialogo||evento.descricao||"").trim().replace(/^["“”]+|["“”]+$/g,"");
        document.getElementById("personagem-dialogo").textContent="“"+fala+"”";
        document.getElementById("personagem-dialogo").removeAttribute("title");
        document.getElementById("evento-contextualizacao").textContent = evento.contextualizacao;
        document.getElementById("evento-contextualizacao").classList.toggle("hidden",!estadoAtual.mostrarNumeros);

        document.getElementById("personagem-nome").textContent = evento.personagemNome || "Conselheiro";
        document.getElementById("personagem-cargo").textContent = evento.personagemCargo || evento.categoria;

        const avatar=evento.personagemAvatar;
        if(retratoAnterior.evento!==evento.id){retratoAnterior={evento:evento.id,avatar:avatar,variante:retratoAnterior.avatar===avatar?1-retratoAnterior.variante:0};}
        document.getElementById("svg-retrato").innerHTML = gerarArteReignsSVG(avatar,retratoAnterior.variante);

        const decA = Object.assign({},evento.decretoA,{titulo:tituloVisual(evento.id,"A",evento.decretoA.titulo)});
        const decB = Object.assign({},evento.decretoB,{titulo:tituloVisual(evento.id,"B",evento.decretoB.titulo)});
        document.getElementById("drag-label-a").textContent="← "+decA.titulo;
        document.getElementById("drag-label-b").textContent=decB.titulo+" →";
        document.getElementById("choice-curtain").classList.remove("visible");
        ["caixa","aprovacao","pib","inflacao"].forEach(function(k){document.getElementById("dot-"+k).className="reigns-dot";});
        document.getElementById("hint-curto-a").textContent = decA.titulo;
        document.getElementById("hint-curto-b").textContent = decB.titulo;

        document.getElementById("dec-a-titulo").textContent = decA.titulo;
        document.getElementById("dec-a-politica").textContent = decA.tipoPolitica;

        document.getElementById("dec-b-titulo").textContent = decB.titulo;
        document.getElementById("dec-b-politica").textContent = decB.tipoPolitica;
        resetarInclinacaoCarta();
        if(animarFlip && inner3d)revelarProximaCarta(inner3d);
    }

    function exibirConsequenciaDoTurno(estado) {
        const hist = estado.historico || [];
        if (hist.length === 0) return;

        exibindoConsequencia = true;
        document.querySelector(".conseq-memoria-details").open=false;
        const u = hist[hist.length - 1];
        const meta=decisaoVisual&&decisaoVisual.turno===u.numeroTurno?decisaoVisual:null;
        document.getElementById("conseq-descricao").textContent=meta?meta.descricao:u.decretoTitulo;
        document.getElementById("conseq-nota").textContent=meta?meta.nota:"";

        document.getElementById("mesa-cartas").classList.add("hidden");
        document.getElementById("tela-fim-de-jogo").classList.add("hidden");
        document.getElementById("carta-consequencia").classList.remove("hidden");

        document.getElementById("conseq-decisao-titulo").textContent =
            tituloVisual(u.eventoId,u.decretoCodigo,u.decretoTitulo);
        document.getElementById("conseq-politica-tipo").textContent =
            u.eventoNome + " • " + u.decretoTipoPolitica;

        preencherPillConseq("caixa", u.deltaCaixa, u.caixaDepois, "", false);
        preencherPillConseq("aprovacao", u.deltaAprovacao, u.aprovacaoDepois, "%", false);
        preencherPillConseq("pib", u.deltaPib, u.pibDepois, "%", false);
        preencherPillConseq("inflacao", u.deltaInflacao, u.inflacaoDepois, "%", true);

        document.getElementById("conseq-sintese").textContent = (u.sinteseDidatica.match(/[^.!?]+[.!?]+(?:\s|$)/g)||[u.sinteseDidatica]).slice(0,2).join(" ").trim();
        document.getElementById("conseq-explicacao").open=false;
        document.getElementById("conseq-porque").textContent = u.porQueMudou;
        document.getElementById("conseq-futuro").textContent = u.custoDecisao;

        const reacoesContainer = document.getElementById("conseq-reacoes-mini");
        reacoesContainer.innerHTML = "";
        (u.reacoesTurno || []).forEach(function (r) {
            const div = document.createElement("div");
            div.className = "mini-reaction-pill";
            div.innerHTML =
                "<strong>" + escapar(r.nomeExibicao) + "</strong>" +
                escapar(r.declaracao);
            reacoesContainer.appendChild(div);
        });

        const listaMem = document.getElementById("lista-memoria-calculo");
        if (listaMem) {
            listaMem.innerHTML = "";
            (u.detalhesCalculo || []).forEach(function (det) {
                const li = document.createElement("li");
                li.textContent = det;
                listaMem.appendChild(li);
            });
        }

        const tituloResultado=document.getElementById("conseq-decisao-titulo");tituloResultado.setAttribute("tabindex","-1");tituloResultado.focus({preventScroll:true});
        const btnProx = document.getElementById("btn-proxima-carta");
        if (estado.partidaEncerrada) {
            btnProx.textContent = estado.vitoria
                ? "Ver Veredito Final da Vitória \u279C"
                : "Ver Queda do Mandato \u279C";
        } else {
            btnProx.textContent = "Virar Próxima Carta (" + estado.turnoAtual + "/" + estado.totalTurnos + ") \u279C";
        }
    }

    function preencherPillConseq(chave, delta, depois, sufixo, inverter) {
        const box = document.getElementById("conseq-box-" + chave);
        document.getElementById("conseq-val-" + chave).textContent = (delta>0?"↑ ":delta<0?"↓ ":"→ ") + formatarSinal(delta) + sufixo;
        document.getElementById("conseq-after-" + chave).textContent = formatarNum(depois) + sufixo;

        let cls = "c-pill";
        if (Math.abs(delta) >= 0.05) {
            let bom = delta > 0;
            if (inverter) bom = delta < 0;
            cls += bom ? " delta-good" : " delta-bad";
        }
        box.className = cls;
        box.style.setProperty("--reveal-order",["caixa","aprovacao","pib","inflacao"].indexOf(chave));
    }

    function fecharConsequenciaEVirarProxima() {
        exibindoConsequencia = false;
        document.getElementById("carta-consequencia").classList.add("hidden");

        if (!estadoAtual) return;
        if (estadoAtual.partidaEncerrada) {
            exibirFimDeJogo(estadoAtual);
        } else {
            document.getElementById("mesa-cartas").classList.remove("hidden");
            renderizarCartaPersonagem(estadoAtual.eventoAtual, true);
        }
    }

    function exibirFimDeJogo(estado) {
        const tela = document.getElementById("tela-fim-de-jogo");
        tela.classList.remove("hidden");
        document.querySelector(".endgame-analysis").open=false;
        tela.dataset.resultado=estado.vitoria?"vitoria":"derrota";
        const tituloFinal=document.getElementById("fim-titulo");tituloFinal.setAttribute("tabindex","-1");tituloFinal.focus({preventScroll:true});
        const avaliacao=estado.avaliacaoFinalEconomica||"";
        document.getElementById("fim-classificacao").textContent=!estado.vitoria?"Mandato interrompido":avaliacao.includes("Estadista")?"Estadista":avaliacao.includes("Pressão Inflacionária")?"Sob pressão":avaliacao.includes("Baixo Crescimento")?"Gestão prudente":"Gestão equilibrada";

        tocarSom(estado.vitoria ? "vitoria" : "derrota");

        const banner = document.getElementById("fim-banner-topo");
        const icone = document.getElementById("fim-icone");
        const badge = document.getElementById("fim-status-badge");

        if (estado.vitoria) {
            banner.className = "endgame-banner vitoria";
            icone.innerHTML = "&#9813;";
            badge.textContent = "VITÓRIA — MANDATO CONCLUÍDO";
        } else {
            banner.className = "endgame-banner derrota";
            icone.innerHTML = "&#9760;";
            badge.textContent = "Derrota · " + ({DERROTA_CAIXA:"Crise fiscal",DERROTA_APROVACAO:"Perda de apoio",DERROTA_AMBOS:"Colapso institucional",DERROTA_HIPERINFLACAO:"Crise de preços",DERROTA_DEPRESSAO:"Depressão econômica"}[estado.statusPartida]||"Mandato interrompido");
        }

        document.getElementById("fim-titulo").textContent = estado.tituloFimDeJogo;
        document.getElementById("fim-motivo").textContent = estado.motivoFimDeJogo;
        document.getElementById("fim-turnos-realizados").textContent = (estado.historico || []).length;
        document.getElementById("fim-avaliacao-economica").textContent = estado.avaliacaoFinalEconomica;

        const grid = document.getElementById("fim-resumo-indicadores");
        grid.innerHTML = "";
        [
            estado.indicadores.caixa,
            estado.indicadores.aprovacao,
            estado.indicadores.pib,
            estado.indicadores.inflacao
        ].forEach(function (ind) {
            const dTotal = ind.valorAtual - ind.valorInicial;
            const div = document.createElement("div");
            div.className = "c-pill";
            div.innerHTML =
                "<span class=\"c-pill-label\">" + escapar(ind.nome) + "</span>" +
                "<strong class=\"c-pill-delta\">" + formatarNum(ind.valorAtual) + "</strong>" +
                "<small>Início: " + formatarNum(ind.valorInicial) + " (" + formatarSinal(dTotal) + ")</small>";
            grid.appendChild(div);
        });

        desenharGrafico("grafico-fim-jogo", estado);
        const h=estado.historico||[], aprendizados=[];
        const gastos=h.filter(function(t){return t.deltaCaixa<0&&t.deltaAprovacao>0;}).length;
        const ajustes=h.filter(function(t){return t.deltaCaixa>0&&t.deltaAprovacao<0;}).length;
        const juros=h.filter(function(t){return t.jurosDepois>t.jurosAntes;}).length;
        if(gastos)aprendizados.push("Em "+gastos+" decisões, o apoio popular cresceu enquanto o caixa caiu: proteger renda exige financiar o custo fiscal.");
        if(ajustes)aprendizados.push("Em "+ajustes+" decisões, o caixa melhorou com perda de aprovação: ajustes econômicos também têm custo político.");
        if(juros)aprendizados.push("Você elevou os juros em "+juros+" turnos. O crédito reage com atraso: as ondas T+1 e T+2 ajudam a explicar o resultado.");
        const maturados=h.reduce(function(n,t){return n+(t.efeitosAtrasadosAtivados||[]).length;},0);
        aprendizados.push(maturados?"Você acompanhou "+maturados+" efeitos defasados: o resultado de um turno também depende de decisões anteriores.":"As decisões de hoje podem produzir efeitos depois do mandato. Consulte sempre a fila antes de escolher.");
        if(aprendizados.length<2)aprendizados.push("O mandato terminou com caixa "+formatarNum(estado.indicadores.caixa.valorAtual)+" e aprovação "+formatarNum(estado.indicadores.aprovacao.valorAtual)+"%: a estabilidade depende de preservar ambas as reservas.");
        const ul=document.getElementById("fim-aprendizados");ul.textContent="";aprendizados.slice(0,3).forEach(function(t){const li=document.createElement("li");li.textContent=t;ul.appendChild(li);});
        document.getElementById("grafico-resumo-texto").textContent=h.map(function(t){return "Turno "+t.numeroTurno+": caixa "+t.caixaDepois+", aprovação "+t.aprovacaoDepois+", PIB "+t.pibDepois+", inflação "+t.inflacaoDepois;}).join(". ");
    }

    function preencherModalCronicas(estado) {
        const contEfeitos = document.getElementById("lista-efeitos-pendentes");
        contEfeitos.innerHTML = "";
        const pend = estado.efeitosPendentes || [];
        if (pend.length === 0) {
            contEfeitos.innerHTML = "<div class=\"drawer-item\">Nenhum efeito atrasado pendente no momento.</div>";
        } else {
            pend.forEach(function (ef) {
                const div = document.createElement("div");
                div.className = "drawer-item";
                div.innerHTML =
                    "<strong>Ativa em " + ef.turnosRestantes + " turno(s) — " + escapar(ef.decretoOrigem) + "</strong><br>" +
                    escapar(ef.descricao);
                contEfeitos.appendChild(div);
            });
        }

        const contReacoes = document.getElementById("lista-reacoes-modal");
        if (contReacoes) {
            contReacoes.innerHTML = "";
            (estado.reacoesAtuais || []).forEach(function (r) {
                const div = document.createElement("div");
                div.className = "drawer-item";
                div.innerHTML =
                    "<strong>" + escapar(r.nomeExibicao) + " — " + escapar(r.titulo) + "</strong><br>" +
                    "<em>" + escapar(r.declaracao) + "</em><br>" +
                    "<small>" + escapar(r.explicacaoRegra) + " | " + escapar(r.efeitoPratico) + "</small>";
                contReacoes.appendChild(div);
            });
        }

        const contHist = document.getElementById("lista-historico");
        contHist.innerHTML = "";
        const hist = estado.historico || [];
        if (hist.length === 0) {
            contHist.innerHTML = "<div class=\"drawer-item\">Nenhuma decisão registrada ainda.</div>";
        } else {
            for (let i = hist.length - 1; i >= 0; i--) {
                const h = hist[i];
                const div = document.createElement("div");
                div.className = "drawer-item";
                div.innerHTML =
                    "<strong>Turno " + h.numeroTurno + ": " + escapar(h.eventoNome) + " &rarr; " + escapar(h.decretoTitulo) + "</strong><br>" +
                    "<span>" + escapar(h.oQueMudou) + "</span><details><summary>Explicação completa</summary><p>"+escapar(h.porQueMudou)+"</p><p>"+escapar(h.custoDecisao)+"</p><p>"+escapar(h.consequenciasFuturas)+"</p></details>";
                contHist.appendChild(div);
            }
        }
    }

    function desenharGrafico(canvasId, estado) {
        const canvas = document.getElementById(canvasId);
        if (!canvas || !canvas.getContext) return;
        const ctx = canvas.getContext("2d");
        const W = canvas.width, H = canvas.height;
        ctx.clearRect(0, 0, W, H);

        const pts = [{
            caixa: estado.historico.length ? estado.historico[0].caixaAntes : estado.indicadores.caixa.valorAtual,
            aprovacao: estado.historico.length ? estado.historico[0].aprovacaoAntes : estado.indicadores.aprovacao.valorAtual,
            pib: estado.historico.length ? estado.historico[0].pibAntes : estado.indicadores.pib.valorAtual,
            inflacao: estado.historico.length ? estado.historico[0].inflacaoAntes : estado.indicadores.inflacao.valorAtual
        }];
        (estado.historico || []).forEach(function (h) {
            pts.push({
                caixa: h.caixaDepois,
                aprovacao: h.aprovacaoDepois,
                pib: h.pibDepois,
                inflacao: h.inflacaoDepois
            });
        });

        const padL = 32, padR = 16, padT = 14, padB = 22;
        const pW = W - padL - padR, pH = H - padT - padB;

        ctx.strokeStyle = "rgba(255,255,255,0.1)";
        [0, 50, 100].forEach(function (n) {
            const y = padT + pH - (n / 100) * pH;
            ctx.beginPath();
            ctx.moveTo(padL, y);
            ctx.lineTo(W - padR, y);
            ctx.stroke();
        });

        const slots = Math.max(estado.totalTurnos, pts.length - 1);
        function xPos(i) { return padL + (i / slots) * pW; }
        function yPos(norm) { return padT + pH - (Math.max(0, Math.min(100, norm)) / 100) * pH; }

        function linha(fn, cor) {
            ctx.strokeStyle = cor;
            ctx.setLineDash(cor === "#38bdf8" ? [] : cor === "#4ade80" ? [8,3] : cor === "#c084fc" ? [2,3] : [10,3,2,3]);
            ctx.lineWidth = 2.4;
            ctx.beginPath();
            pts.forEach(function (p, i) {
                const x = xPos(i), y = fn(p);
                if (i === 0) ctx.moveTo(x, y);
                else ctx.lineTo(x, y);
            });
            ctx.stroke();
        }

        linha(function (p) { return yPos((p.caixa / 150) * 100); }, "#38bdf8");
        linha(function (p) { return yPos(p.aprovacao); }, "#4ade80");
        linha(function (p) { return yPos(((p.pib + 6) / 16) * 100); }, "#c084fc");
        linha(function (p) { return yPos((p.inflacao / 20) * 100); }, "#f87171");
    }

    // =========================================================================
    // ARTE VETORIAL GEOMÉTRICA BIPARTIDA FIEL A REIGNS (LOW-POLY / FLAT VECTOR)
    // =========================================================================
    function gerarArteReignsSVG(avatar,variante) {
        const retratos={
            AGRICULTORA:{fundo:"#a89e7c",sombra:"#7f7658",roupa:"#6d7961",acento:"#d7c38d",cabelo:"#645344",pele:"#b58e6c",tipo:"agricultura",rosto:"101,111 159,91 215,114 202,214 162,238 113,210"},
            ECONOMISTA_OCULOS:{fundo:"#87938e",sombra:"#626f68",roupa:"#45524d",acento:"#c5ba9b",cabelo:"#aaafa1",pele:"#c8a987",tipo:"economista",rosto:"116,103 164,85 209,110 194,214 160,245 119,210"},
            PREFEITA_COLETE:{fundo:"#a69a8c",sombra:"#7e7164",roupa:"#6d7575",acento:"#ccba94",cabelo:"#514740",pele:"#d2ae8b",tipo:"prefeita",rosto:"101,113 158,93 214,111 206,213 160,238 112,212"},
            MINISTRA_TAILLEUR:{fundo:"#a38e99",sombra:"#7c6472",roupa:"#6d5967",acento:"#d3b992",cabelo:"#493a40",pele:"#bc9178",tipo:"ministra",rosto:"109,111 161,89 211,111 196,213 159,240 120,212"},
            OFICIAL_PORTOS:{fundo:"#85959c",sombra:"#5b6c76",roupa:"#3c4a57",acento:"#cbb87d",cabelo:"#b9bbaa",pele:"#c6a07c",tipo:"oficial",rosto:"104,112 161,89 216,111 204,213 159,239 114,216"},
            MINISTRO_SAUDE:{fundo:"#758f86",sombra:"#586e66",roupa:"#e0daca",acento:"#8aab98",cabelo:"#adb2a5",pele:"#d7b697",tipo:"saude",rosto:"106,108 160,91 211,111 203,205 162,237 115,214"},
            PRESIDENTE_BC:{fundo:"#7d8798",sombra:"#5a6477",roupa:"#373e49",acento:"#cbb77f",cabelo:"#51473e",pele:"#d6b595",tipo:"banco",rosto:"113,105 163,89 206,111 194,213 157,242 118,207"},
            POPULACAO:{fundo:"#aa8e79",sombra:"#806757",roupa:"#b9a68c",acento:"#d9c6a0",cabelo:"#5b493b",pele:"#b88c6c",tipo:"povo",rosto:"99,109 157,88 217,109 212,208 163,235 107,209"},
            ENERGIA:{fundo:"#a3916b",sombra:"#7c6b4c",roupa:"#697781",acento:"#d4bd85",cabelo:"#45433b",pele:"#d2aa86",tipo:"energia",rosto:"103,109 160,91 217,109 204,213 161,239 114,215"},
            TRABALHADORES:{fundo:"#9c817b",sombra:"#765e59",roupa:"#68767d",acento:"#ba8b7d",cabelo:"#494039",pele:"#b58b6b",tipo:"trabalho",rosto:"96,115 159,91 224,116 211,216 160,239 107,214"},
            MINISTRO_ECONOMIA:{fundo:"#8e8b79",sombra:"#686657",roupa:"#414a4b",acento:"#c4b68c",cabelo:"#c1bba9",pele:"#d6b391",tipo:"economia",rosto:"110,111 164,89 213,113 198,210 162,241 119,209"},
            CHANCELER:{fundo:"#8e8299",sombra:"#6c6077",roupa:"#45424e",acento:"#c6b084",cabelo:"#c9c2af",pele:"#c7a17e",tipo:"chanceler",rosto:"112,102 161,86 210,107 194,219 158,246 120,213"},
            OPOSICAO:{fundo:"#9e7d83",sombra:"#76565e",roupa:"#45404b",acento:"#c49a92",cabelo:"#443e43",pele:"#d2ad90",tipo:"oposicao",rosto:"109,112 158,88 215,109 199,216 157,248 120,210"},
            EMPRESARIO:{fundo:"#829184",sombra:"#5e6e61",roupa:"#4b6058",acento:"#b6c0a1",cabelo:"#71604b",pele:"#d2ac8c",tipo:"empresa",rosto:"104,105 158,86 214,109 201,214 160,238 113,210"},
            MINISTRA_EDUCACAO:{fundo:"#978d9f",sombra:"#74677e",roupa:"#706178",acento:"#d6c397",cabelo:"#453d3e",pele:"#b98d6e",tipo:"educacao",rosto:"111,110 163,91 210,111 201,210 161,235 120,211"},
            GOVERNADOR:{fundo:"#849196",sombra:"#606c72",roupa:"#47545b",acento:"#a8b391",cabelo:"#989e97",pele:"#cba27e",tipo:"governador",rosto:"98,110 160,88 218,111 210,216 160,239 107,216"}
        };
        const r=retratos[avatar]||retratos.MINISTRO_ECONOMIA;
        const escuro="#302d30",luz="#eee4d1";
        let acessorio="",cabelo="<path d='M99 124L110 82 160 63 211 82 222 124 195 102 122 105Z' fill='"+r.cabelo+"'/>";
        if(r.tipo==="educacao")cabelo="<path d='M95 240V114L105 77 161 63 214 82 227 133 230 247 199 226 203 113 122 110 120 225Z' fill='"+r.cabelo+"'/>";
        if(r.tipo==="saude")cabelo="<path d='M100 126L112 87 132 82 124 116 112 152Z M191 84L213 96 222 129 209 151 205 117Z' fill='"+r.cabelo+"'/>";
        if(r.tipo==="energia")acessorio="<path d='M90 116L103 85 145 65H180L216 90 229 116Z' fill='"+r.acento+"'/><path d='M83 113H237V125H83Z' fill='#ac9463'/><path d='M157 70H170V112H157Z' fill='"+luz+"'/>";
        if(r.tipo==="trabalho")acessorio="<path d='M93 117L109 81 167 70 217 98 225 119 153 112 94 130Z' fill='"+r.acento+"'/>";
        if(r.tipo==="banco"||r.tipo==="educacao")acessorio+="<path d='M113 143H149V165H113Z M173 143H209V165H173Z M149 150H173' fill='none' stroke='"+r.acento+"' stroke-width='5'/>";
        if(r.tipo==="saude")acessorio="<path d='M119 260V288L141 310H180L200 288V260' fill='none' stroke='"+r.acento+"' stroke-width='7'/><path d='M153 308H169V324H153Z M213 281H235V286H213Z' fill='"+r.acento+"'/>";
        if(r.tipo==="oposicao")acessorio="<path d='M119 199L159 217 202 197 189 230 158 250 131 228Z' fill='"+r.cabelo+"'/>";
        if(r.tipo==="povo")acessorio="<path d='M70 278L125 246 163 279 197 247 250 278 204 321 157 300 108 322Z' fill='"+r.acento+"'/>";
        if(r.tipo==="chanceler"||r.tipo==="governador")acessorio="<path d='M91 267L120 251 236 365H199Z' fill='"+r.acento+"'/>";
        if(r.tipo==="empresa")acessorio="<path d='M208 314H277V365H208Z M227 314V302H257V314' fill='"+r.acento+"' stroke='"+r.roupa+"' stroke-width='5'/>";
        if(r.tipo==="educacao")acessorio+="<path d='M197 294L251 283 264 348 211 358Z' fill='"+r.acento+"'/><path d='M204 300L219 354' stroke='"+r.roupa+"' stroke-width='3'/>";
        if(r.tipo==="economia")acessorio="<path d='M92 283H117V298H92Z M89 280H121' stroke='"+r.acento+"' fill='none' stroke-width='3'/>";
        if(r.tipo==="agricultura")acessorio="<path d='M69 120L113 103 128 65H187L206 103 252 120 230 134H89Z' fill='"+r.acento+"'/><path d='M112 103H206L213 116H105Z' fill='#7b6950'/><path d='M78 279L119 255 160 284 201 255 242 279 210 317 158 304 108 319Z' fill='"+r.acento+"'/>";
        if(r.tipo==="economista")acessorio="<path d='M113 142H150V165H113Z M172 142H208V165H172Z M150 151H172' fill='none' stroke='"+r.acento+"' stroke-width='5'/><path d='M206 295L249 291 255 352 211 357Z' fill='"+r.acento+"'/><path d='M215 310H241M216 320H241M218 330H243' stroke='"+r.roupa+"' stroke-width='3'/>";
        if(r.tipo==="prefeita")acessorio="<path d='M82 270L127 251 150 365H43Z M190 252L239 273 280 365H169Z' fill='"+r.acento+"'/><path d='M88 297H126V321H88Z M200 297H236V321H200Z' fill='"+r.roupa+"'/><path d='M155 287V365' stroke='"+r.roupa+"' stroke-width='4'/>";
        if(r.tipo==="ministra"){cabelo="<path d='M96 222V109L107 77 162 65 212 86 224 123 224 222 201 211 202 112 122 115 118 213Z' fill='"+r.cabelo+"'/>";acessorio="<path d='M89 269L132 251 158 301 125 286 112 305Z M187 251L233 269 211 305 195 285 162 301Z' fill='"+r.acento+"'/><path d='M211 277L219 285 211 293 203 285Z' fill='"+r.acento+"'/>";}
        if(r.tipo==="oficial")acessorio="<path d='M90 108L104 76 160 65 216 76 231 108Z' fill='"+r.roupa+"'/><path d='M86 105H234V119H86Z' fill='"+r.acento+"'/><path d='M103 119H219L202 130H122Z' fill='"+escuro+"'/><path d='M71 279L100 263 120 278 109 290Z M200 278L220 263 249 279 211 290Z M208 309H236V316H208Z' fill='"+r.acento+"'/>";
        return "<svg viewBox='0 0 320 365' xmlns='http://www.w3.org/2000/svg' aria-hidden='true'><g transform='"+(variante?"translate(320 0) scale(-1 1)":"translate(0 0)")+"'>"+
            "<path d='M0 0H320V365H0Z' fill='"+r.fundo+"'/><path d='M160 0H320V365H160Z' fill='"+r.sombra+"'/>"+
            "<path d='M35 365L84 268 138 250H184L237 272 287 365Z' fill='"+r.roupa+"'/><path d='M137 216H184V267H137Z' fill='"+r.pele+"'/><path d='M125 254L160 289 196 254 175 314H148Z' fill='"+luz+"'/><path d='M155 281H170L178 365H147Z' fill='"+r.acento+"'/>"+
            "<polygon points='"+r.rosto+"' fill='"+r.pele+"'/><path d='M161 94L213 111 201 212 160 239Z' fill='"+r.sombra+"' opacity='.23'/>"+cabelo+
            "<path d='M129 145H135V168H129Z M185 145H191V168H185Z' fill='"+escuro+"'/>"+acessorio+"</g></svg>";
    }

    function formatarNum(v) {
        if (v === undefined || v === null || isNaN(v)) return "0,0";
        return Number(v).toFixed(1).replace(".", ",");
    }

    function formatarSinal(v) {
        const n = Number(v || 0);
        const s = n.toFixed(1).replace(".", ",");
        return n > 0 ? "+" + s : s;
    }

    function escapar(str) {
        if (!str) return "";
        return String(str)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;");
    }
})();
