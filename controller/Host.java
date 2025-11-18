/***************************************************************** 
* Autor............: Lucas de Menezes Chaves
* Matricula........: 202310282
* Inicio...........: 17/11/2025
* Ultima alteracao.: 17/11/2025
* Nome.............: Host
* Funcao...........: representa um no na rede
*************************************************************** */

package controller;

import model.AplicacaoReceptora;
import model.AplicacaoTransmissora;
import model.CamadaAplicacaoReceptora;
import model.CamadaAplicacaoTransmissora;
import model.CamadaEnlaceDadosReceptora;
import model.CamadaEnlaceDadosTransmissora;
import model.CamadaFisicaReceptora;
import model.CamadaFisicaTransmissora;
import model.MeioDeComunicacao;
import util.Erro;

public class Host {
  //variaveis da pilha transmissora
  public AplicacaoTransmissora aplicacaoTransmissora;
  public CamadaAplicacaoTransmissora camadaAplicacaoTransmissora;
  public CamadaEnlaceDadosTransmissora camadaEnlaceDadosTransmissora;
  public CamadaFisicaTransmissora camadaFisicaTransmissora;

  //variaveis da pilha receptora
  public AplicacaoReceptora aplicacaoReceptora;
  public CamadaAplicacaoReceptora camadaAplicacaoReceptora;
  public CamadaEnlaceDadosReceptora camadaEnlaceDadosReceptora;
  public CamadaFisicaReceptora camadaFisicaReceptora;

  /**************************************************************
  * Metodo: Host
  * Funcao: monta as pilhas referentes ao host
  * @param controller | controller do projeto
  * @param nome | nome do host
  * @param target | aplicacao receptora alvo
  * @return void
  * ********************************************************* */
  public Host(TelaPrincipalController controller, String nome, AplicacaoReceptora target) {
    this.aplicacaoReceptora = target;
    this.camadaAplicacaoReceptora = new CamadaAplicacaoReceptora(this.aplicacaoReceptora, controller);
		this.camadaEnlaceDadosReceptora = new CamadaEnlaceDadosReceptora(this.camadaAplicacaoReceptora, controller);
		this.camadaFisicaReceptora = new CamadaFisicaReceptora(this.camadaEnlaceDadosReceptora, controller);

		// montando pilha transmissora
		this.camadaFisicaTransmissora = new CamadaFisicaTransmissora(controller);
		this.camadaEnlaceDadosTransmissora = new CamadaEnlaceDadosTransmissora(this.camadaFisicaTransmissora, controller);
		this.camadaAplicacaoTransmissora = new CamadaAplicacaoTransmissora(this.camadaEnlaceDadosTransmissora, controller);
		this.aplicacaoTransmissora = new AplicacaoTransmissora(this.camadaAplicacaoTransmissora);

		// conexao para que a camada fisica saiba quem eh a superior
		this.camadaFisicaTransmissora.setCamadaEnlaceSuperior(this.camadaEnlaceDadosTransmissora);

		// conexoes virtuais para que as camdas irmas se conhecam, saibam onde o ACK tem que chegar
		this.camadaEnlaceDadosReceptora.setCamadaEnlaceTransmissoraIrma(this.camadaEnlaceDadosTransmissora);

		// A Camada Enlace (RX) precisa enviar ACKs de volta usando a sua propria pilha de transmissao (TX).
		this.camadaEnlaceDadosReceptora.setAplicacaoTransmissoraIrma(this.aplicacaoTransmissora);
  } // fim da classe

  /**************************************************************
  * Metodo: setMeioDeComunicacao
  * Funcao: "ajeita" o meio de comunicacao
  * @param meio | meio de comunicacao que vai ser setado
  * @return void
  * ********************************************************* */
  public void setMeioDeComunicacao(MeioDeComunicacao meio) {
    //this.camadaFisicaTransmissora.setMeioDeComunicacao(meio);
		//this.camadaFisicaReceptora.setMeioDeComunicacao(meio);
  } // fim do metodo

  /**************************************************************
  * Metodo: enviar
  * Funcao: envia a mensagem
  * @param mensagem | mensagem digitada
  * @return void
  * ********************************************************* */
  public void enviar(String mensagem) throws Erro {
    //this.aplicacaoTransmissora.transmitir(mensagem);
  } // fim do metodo
} // fim da classe
