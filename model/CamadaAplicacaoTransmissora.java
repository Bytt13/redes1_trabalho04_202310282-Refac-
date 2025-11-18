/***************************************************************** 
* Autor..............: Lucas de Menezes Chaves
* Matricula........: 202310282
* Inicio...........: 18/08/2025
* Ultima alteracao.: 31/10/2025
* Nome.............: CamadaDeAplicacaoTransmissora
* Funcao...........: Transfere a mensagem em binario para camada seguinte
*************************************************************** */
package model;

import controller.TelaPrincipalController;
import javafx.application.Platform;
import util.Erro;
import util.Auxiliares;

public class CamadaAplicacaoTransmissora {
  private CamadaEnlaceDadosTransmissora camadaEnlaceDadosTransmissora;
  private TelaPrincipalController controller;

  /**************************************************************
  * Metodo: CamadaAplicacaoTransmissora
  * Funcao: contrutor com referencia a aplicacao tx e ao controller
  * @param camadaEnlaceDadosTransmissora | instancia da camada de enlace de dados transmissora
  * @param controller | instancia do controller
  * @return void 
  * ********************************************************* */
  public CamadaAplicacaoTransmissora(CamadaEnlaceDadosTransmissora camadaEnlaceDadosTransmissora, TelaPrincipalController controller) {
    this.camadaEnlaceDadosTransmissora = camadaEnlaceDadosTransmissora;
    this.controller = controller;
  } // fim do construtor

  /**************************************************************
  * Metodo: transmitir
  * Funcao: envia a mensagem e passa para a proxima camada
  * @param mensagem | mensagem que vai ser transmitidas
  * @return void 
  * ********************************************************* */
  public void transmitir(String mensagem) throws Erro {
    int[] frame = Auxiliares.stringToInt(mensagem);
    Platform.runLater(() -> {
      this.controller.exibirMensagemBinariaTransmitida(frame);
    });

    //verificacao para o caso de ack
    if(mensagem.equals("ACK")) {
      this.camadaEnlaceDadosTransmissora.transmitirACK(frame);
    } else {
      this.camadaEnlaceDadosTransmissora.transmitir(frame);
    }
  } // fim do metodo
} // Fim da classe