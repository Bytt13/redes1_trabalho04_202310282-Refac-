/***************************************************************** 
* Autor..............: Lucas de Menezes Chaves
* Matricula........: 202310282
* Inicio...........: 19/08/2025
* Ultima alteracao.: 18/11/2025
* Nome.............: CamadaFisicaTransmissora
* Funcao...........: Codifica os bits da mensagem recebida e envia ao meio
*************************************************************** */

package model;

import controller.TelaPrincipalController;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import util.Erro;
import util.Auxiliares;

public class CamadaFisicaTransmissora {

  private TelaPrincipalController controller; // referencia para a interface grafica
  private MeioDeComunicacao meioDeComunicacao; // referencia para o meio de comunicacao
  private CamadaEnlaceDadosTransmissora camadaEnlaceDadosTransmissora; // referencia a classe superior

  /**************************************************************
  * Metodo: CamadaFisicaTransmissora
  * Funcao: construtor da classe
  * @param controller | referencia para controle da interface
  * @return void 
  * ********************************************************* */
  public CamadaFisicaTransmissora(TelaPrincipalController controller) {
    this.controller = controller;
  } // fim do construtor

  /**************************************************************
  * Metodo: setMeioDeComunicacao
  * Funcao: define o meio de comunicacao que sera utilizado
  * @param meioDeComunicacao | o meio de comunicacao
  * @return void 
  * ********************************************************* */
  public void setMeioDeComunicacao(MeioDeComunicacao meioDeComunicacao) {
    this.meioDeComunicacao = meioDeComunicacao;
  } // fim do metodo

  /**************************************************************
  * Metodo: setCamadaEnlaceSuperior
  * Funcao: define qual a camada superior a essa
  * @param camadaEnlaceDadosTransmissora | a camada superior
  * @return void 
  * ********************************************************* */
  public void setCamadaEnlaceSuperior(CamadaEnlaceDadosTransmissora camadaEnlaceDadosTransmissora) {
    this.camadaEnlaceDadosTransmissora = camadaEnlaceDadosTransmissora;
  } // fim do metodo

  /**************************************************************
  * Metodo: transmitir
  * Funcao: codifica o quadro e o envia para o meio
  * @param quadro | o quadro de bits vindo da enlace
  * @return void 
  * ********************************************************* */
  public void transmitir(int[] quadro) throws Erro {
    int tipoDeCodificacao = this.controller.codCodification();
    int tipoDeEnquadramento = this.controller.enquadCodification();
    int[] fluxoBrutoDeBits = null; // sinal que sera enviado

    // Verificacao de erro: Binario nao suporta Violacao da Camada Fisica
    if (tipoDeCodificacao == 0 && tipoDeEnquadramento == 3) {
      Platform.runLater(() -> {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Erro de Compatibilidade");
        alert.setHeaderText("COMBINAÇÃO INVÁLIDA");
        alert.setContentText("Nao é possivel utilizar Codificacao Binaria com Violacao da Camada Fisica.");
        alert.showAndWait();

        if(this.camadaEnlaceDadosTransmissora != null) { 
            camadaEnlaceDadosTransmissora.abortarTransmissao();
        }
      });
      return; // Aborta
    } // fim do if

    // Logica de codificacao
    if (tipoDeEnquadramento == 3) { // Se for Violacao da Camada Fisica
      fluxoBrutoDeBits = camadaFisicaTransmissoraComViolacao(quadro, tipoDeCodificacao);
    } else {
      switch (tipoDeCodificacao) {
        case 0: // Binario
          fluxoBrutoDeBits = camadaFisicaTransmissoraCodificacaoBinaria(quadro);
          break;
        case 1: // Manchester
          fluxoBrutoDeBits = camadaFisicaTransmissoraCodificacaoManchester(quadro);
          break;
        case 2: // Manchester Diferencial
          fluxoBrutoDeBits = camadaFisicaTransmissoraCodificacaoManchesterDiferencial(quadro);
          break;
      } // fim do switch
    } // fim do else

    // Manda pra proxima etapa (Meio de Comunicacao)
    // O meio ja desenha o sinal na tela atraves do controller
    if (fluxoBrutoDeBits != null) {
      this.meioDeComunicacao.transmitir(fluxoBrutoDeBits, this);
    }
  } // fim do metodo

  /**************************************************************
  * Metodo: camadaFisicaTransmissoraCodificacaoBinaria
  * Funcao: aplica a codificacao binaria (sem alteracao)
  * @param quadro | quadro original
  * @return int[] | quadro codificado
  * ********************************************************* */
  public int[] camadaFisicaTransmissoraCodificacaoBinaria(int[] quadro) {
    return quadro;
  } // fim do metodo

  /**************************************************************
  * Metodo: camadaFisicaTransmissoraCodificacaoManchester
  * Funcao: aplica codificacao Manchester (1->10, 0->01)
  * @param quadro | quadro original
  * @return int[] | quadro codificado
  * ********************************************************* */
  public int[] camadaFisicaTransmissoraCodificacaoManchester(int[] quadro) {
    int totalBits = Auxiliares.tratarBits(quadro); // usa a classe util Auxiliares
    int totalBitsManchester = totalBits * 2;
    int tamanhoArrayManchester = (totalBitsManchester + 31) / 32;

    int[] pacoteManchester = new int[tamanhoArrayManchester];

    for (int i = 0; i < totalBits; i++) {
      int bitOriginal = Auxiliares.lerBits(quadro, i, 1);
      int sinal1, sinal2;

      if (bitOriginal == 1) {
        sinal1 = 1;
        sinal2 = 0;
      } else {
        sinal1 = 0;
        sinal2 = 1;
      }

      // Escreve os dois bits do sinal no novo pacote
      Auxiliares.escreverBits(pacoteManchester, i * 2, sinal1, 1);
      Auxiliares.escreverBits(pacoteManchester, (i * 2) + 1, sinal2, 1);
    } // fim for

    return pacoteManchester;
  } // fim do metodo

  /**************************************************************
  * Metodo: camadaFisicaTransmissoraCodificacaoManchesterDiferencial
  * Funcao: aplica codificacao Manchester Diferencial
  * @param quadro | quadro original
  * @return int[] | quadro codificado
  * ********************************************************* */
  public int[] camadaFisicaTransmissoraCodificacaoManchesterDiferencial(int[] quadro) {
    int totalBits = Auxiliares.tratarBits(quadro);
    int totalBitsDiferencial = totalBits * 2;
    int tamanhoArrayDiferencial = (totalBitsDiferencial + 31) / 32;

    int[] pacoteManchesterDiferencial = new int[tamanhoArrayDiferencial];
    int nivelAtual = 1; // estado inicial do sinal

    for (int i = 0; i < totalBits; i++) {
      int bitOriginal = Auxiliares.lerBits(quadro, i, 1);

      // Se bit 0, inverte no inicio do intervalo. Se 1, mantem.
      if (bitOriginal == 0) {
        nivelAtual = 1 - nivelAtual;
      }

      int sinal1 = nivelAtual;
      
      // Transicao obrigatoria no meio do intervalo
      nivelAtual = 1 - nivelAtual; 
      int sinal2 = nivelAtual;

      Auxiliares.escreverBits(pacoteManchesterDiferencial, i * 2, sinal1, 1);
      Auxiliares.escreverBits(pacoteManchesterDiferencial, (i * 2) + 1, sinal2, 1);
    } // fim for

    return pacoteManchesterDiferencial;
  } // fim do metodo

  /**************************************************************
  * Metodo: camadaFisicaTransmissoraComViolacao
  * Funcao: codifica com violacao (flags de inicio/fim)
  * @param quadro | quadro original
  * @param tipoDeCodificacao | tipo escolhido (Manchester ou Dif.)
  * @return int[] | quadro com violacao
  * ********************************************************* */
  private int[] camadaFisicaTransmissoraComViolacao(int[] quadro, int tipoDeCodificacao) {
    final int VIOLACAO = 0b1111; // Flag de violacao simulada
    final int TAMANHO_VIOLACAO_BITS = 4;
    final int TAMANHO_SUBQUADRO_EM_BITS = 32;

    int totalBitsMensagem = Auxiliares.tratarBits(quadro);
    if (totalBitsMensagem == 0) return new int[0];

    // Estimativa de tamanho
    int numSubquadros = (totalBitsMensagem + TAMANHO_SUBQUADRO_EM_BITS - 1) / TAMANHO_SUBQUADRO_EM_BITS;
    int bitsEstimados = (TAMANHO_VIOLACAO_BITS * (numSubquadros + 1)) + (totalBitsMensagem * 2);
    int[] buffer = new int[(bitsEstimados + 31) / 32];
    int cursorEscrita = 0;

    // Escreve Violacao Inicial
    Auxiliares.escreverBits(buffer, cursorEscrita, VIOLACAO, TAMANHO_VIOLACAO_BITS);
    cursorEscrita += TAMANHO_VIOLACAO_BITS;

    int nivelAtual = 1; // Para o Manchester Diferencial
    int contadorBitsSubquadro = 0;

    for (int i = 0; i < totalBitsMensagem; i++) {
      int bitOriginal = Auxiliares.lerBits(quadro, i, 1);
      int sinal1, sinal2;

      if (tipoDeCodificacao == 1) { // Manchester
        sinal1 = (bitOriginal == 1) ? 1 : 0;
        sinal2 = (bitOriginal == 1) ? 0 : 1;
      } else { // Manchester Diferencial
        if (bitOriginal == 0) nivelAtual = 1 - nivelAtual;
        sinal1 = nivelAtual;
        nivelAtual = 1 - nivelAtual;
        sinal2 = nivelAtual;
      }

      Auxiliares.escreverBits(buffer, cursorEscrita++, sinal1, 1);
      Auxiliares.escreverBits(buffer, cursorEscrita++, sinal2, 1);

      contadorBitsSubquadro++;

      // Verifica se precisa inserir violacao (fim de subquadro ou fim de mensagem)
      boolean fimSubquadro = (contadorBitsSubquadro == TAMANHO_SUBQUADRO_EM_BITS);
      boolean fimMensagem = (i == totalBitsMensagem - 1);

      if (fimSubquadro || fimMensagem) {
        Auxiliares.escreverBits(buffer, cursorEscrita, VIOLACAO, TAMANHO_VIOLACAO_BITS);
        cursorEscrita += TAMANHO_VIOLACAO_BITS;
        contadorBitsSubquadro = 0;
        nivelAtual = 1; // Reseta para consistencia
      }
    }

    // Ajusta array final
    int tamanhoFinal = (cursorEscrita + 31) / 32;
    int[] fluxoFinal = new int[tamanhoFinal];
    
    // Copia bit a bit (ou poderia usar System.arraycopy se alinhado, mas bit a bit eh mais seguro aqui)
    for(int i=0; i<cursorEscrita; i++){
      int bit = Auxiliares.lerBits(buffer, i, 1);
      Auxiliares.escreverBits(fluxoFinal, i, bit, 1);
    }

    return fluxoFinal;
  } // fim do metodo
} // Fim da classe