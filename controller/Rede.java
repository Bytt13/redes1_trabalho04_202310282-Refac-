/***************************************************************** 
* Autor............: Lucas de Menezes Chaves
* Matricula........: 202310282
* Inicio...........: 17/11/2025
* Ultima alteracao.: 17/11/2025
* Nome.............: Rede
* Funcao...........: gerencia a comunicacao entre camadas da rede
*************************************************************** */
package controller;

import model.AplicacaoReceptora;
import model.MeioDeComunicacao;
import util.Erro;

public class Rede {
  // referencia da UI e hosts
  private TelaPrincipalController controller;
  private Host hostA;
  private Host hostB;

  //fluxos de transferencia
  private AplicacaoReceptora aParaB;
  private AplicacaoReceptora bParaA;

  //meio de comunicacao
  private MeioDeComunicacao mdc;

  public Rede(TelaPrincipalController controller) {
    //faz todas as instancias
    this.controller = controller;
    this.aParaB = new AplicacaoReceptora(this.controller);
    this.bParaA = new AplicacaoReceptora(null);
    this.hostA = new Host(this.controller, "HostA", this.bParaA);
    this.hostB = new Host(this.controller, "Host B", this.aParaB);
    this.mdc = new MeioDeComunicacao(this.hostA.camadaFisicaTransmissora, this.hostA.camadaFisicaReceptora,
    this.hostB.camadaFisicaTransmissora, this.hostB.camadaFisicaReceptora, this.controller);
    this.hostA.setMeioDeComunicacao(this.mdc);
    this.hostB.setMeioDeComunicacao(this.mdc);
  } // fim do construtor

  /**************************************************************
  * Metodo: startSim
  * Funcao: inicia o envio de mensagens
  * @param mensagem | mensagem que quer enviar
  * @return void 
  * ********************************************************* */
  public void startSim(String mensagem) throws Erro {
    this.hostA.enviar(mensagem);
  } // fim do metodo
}
