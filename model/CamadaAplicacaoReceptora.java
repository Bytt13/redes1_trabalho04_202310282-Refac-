/***************************************************************** 
* Autor..............: Lucas de Menezes Chaves
* Matricula........: 202310282
* Inicio...........: 22/08/2025
* Ultima alteracao.: 31/10/2025
* Nome.............: CamadaDeAplicacaoReceptora
* Funcao...........: Transfere a mensagem convertida de binario para texto para aplicacao receptora
*************************************************************** */

package model;

import controller.TelaPrincipalController;
import util.Auxiliares;

public class CamadaAplicacaoReceptora {
  private AplicacaoReceptora aplicacaoReceptora;
  private TelaPrincipalController controller;

/**************************************************************
  * Metodo: CamadaAplicacaoTransmissora
  * Funcao: contrutor com referencia a aplicacao tx e ao controller
  * @param aplicacaoReceptora | instancia da AplicacaoReceptora
  * @param controller | instancia do controller
  * @return void 
  * ********************************************************* */
  public CamadaAplicacaoReceptora(AplicacaoReceptora aplicacaoReceptora, TelaPrincipalController controller) {
    this.aplicacaoReceptora = aplicacaoReceptora;
    this.controller = controller;
  } // fim do construtor
  /**************************************************************
  * Metodo: receber
  * Funcao: exibe a mensagem e passa para a proxima camada
  * @param quadro | quadro recebido
  * @return void 
  * ********************************************************* */
  public void receber(int[] quadro) {
    this.controller.exibirMensagemBinariaRecebida(quadro);
    String mensagem = Auxiliares.packToString(quadro);
    this.aplicacaoReceptora.receber(mensagem);
  } 
} // fim da classe