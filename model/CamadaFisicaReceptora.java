/*****************************************************************
* Autor..............: Lucas de Menezes Chaves
* Matricula........: 202310282
* Inicio...........: 21/08/2025
* Ultima alteracao.: 18/11/2025
* Nome.............: CamadaFisicaReceptora
* Funcao...........: Recebe o sinal do meio, decodifica e envia para a camada superior
*************************************************************** */
package model;

import controller.TelaPrincipalController;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import util.Erro;
import util.Auxiliares;

public class CamadaFisicaReceptora {

  private CamadaEnlaceDadosReceptora camadaEnlaceDadosReceptora; // referencia a camada superior
  private TelaPrincipalController controller; // referencia para a interface grafica

  /**************************************************************
  * Metodo: CamadaFisicaReceptora
  * Funcao: construtor da classe
  * @param camadaEnlaceDadosReceptora | referencia para a camada superior
  * @param controller | referencia para o controle da interface
  * @return void
  * ********************************************************* */
  public CamadaFisicaReceptora(CamadaEnlaceDadosReceptora camadaEnlaceDadosReceptora, TelaPrincipalController controller) {
    this.camadaEnlaceDadosReceptora = camadaEnlaceDadosReceptora;
    this.controller = controller;
  } // fim construtor

  /**************************************************************
  * Metodo: setMeioDeComunicacao
  * Funcao: define o meio de comunicacao que sera utilizado
  * @param meioDeComunicacao | meio de comunicacao
  * @return void
  * ********************************************************* */
  public void setMeioDeComunicacao(MeioDeComunicacao meioDeComunicacao) {
    // Mantido por compatibilidade com a inicializacao da Rede
  } // fim do metodo

  /**************************************************************
  * Metodo: receber
  * Funcao: recebe o quadro, decodifica e envia para a proxima camada
  * @param quadro | representacao em bits do sinal transmitido
  * @return void
  * ********************************************************* */
  public void receber(int[] quadro) throws Erro {

    // Exibe o sinal recebido na area de decodificada (lado RX)
    this.controller.exibirMensagemBinariaRecebida(quadro);

    int tipoDeEnquadramento = this.controller.enquadCodification();
    int tipoDeDecodificacao = this.controller.codCodification();
    int[] fluxoBrutoDeBits = null;

    try {
      if (tipoDeEnquadramento == 3) {
        fluxoBrutoDeBits = camadaFisicaReceptoraDecodificacaoComViolacao(quadro, tipoDeDecodificacao);
      } else {
        switch (tipoDeDecodificacao) {
          case 0: // codificao binaria
            fluxoBrutoDeBits = camadaFisicaReceptoraDecodificacaoBinaria(quadro);
            break;
          case 1: // codificacao manchester
            fluxoBrutoDeBits = camadaFisicaReceptoraDecodificacaoManchester(quadro);
            break;
          case 2: // codificacao manchester diferencial
            fluxoBrutoDeBits = camadaFisicaReceptoraDecodificacaoManchesterDiferencial(quadro);
            break;
        } // fim do switch/case
      } // fim if/else
    } catch (Erro e) {
      // A decodificacao FALHOU (Ex: Manchester invalido 00 ou 11)
      System.out.println("Camada Fisica Receptora: ERRO DETECTADO. ");

      // Informar o usuario com o Alerta personalizado
      Platform.runLater(() -> {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Deteccao de Erro");
        alert.setHeaderText(e.getHeader());
        alert.setContentText(e.getContent());
        alert.show();
      });
      return; // Descarta o quadro, nao envia para a camada de Enlace
    }

    // chama proxima camada
    this.camadaEnlaceDadosReceptora.receber(fluxoBrutoDeBits);
  }// fim do metodo receber

  /**************************************************************
  * Metodo: camadaFisicaReceptoraDecodificacaoBinaria
  * Funcao: decodifica o Binario (retorna o mesmo array)
  * @param quadro | conjunto de bits recebido
  * @return int[] | o conjunto de bits decodificado
  * ********************************************************* */
  public int[] camadaFisicaReceptoraDecodificacaoBinaria(int[] quadro) {
    return quadro;
  } // fim do metodo

  /**************************************************************
  * Metodo: camadaFisicaReceptoraDecodificacaoManchester
  * Funcao: decodifica Manchester (10 -> 1, 01 -> 0)
  * @param quadro | array com bits codificados
  * @return int[] | array decodificado
  * @throws Erro | trata erros de violacao dos pares de sinal
  * ********************************************************* */
  public int[] camadaFisicaReceptoraDecodificacaoManchester(int[] quadro) throws Erro {

    int totalBitsManchester = Auxiliares.tratarBits(quadro);
    if (totalBitsManchester % 2 != 0) {
      throw new Erro("ERRO NA CAMADA FISICA (MANCHESTER)",
          "O sinal recebido tem um numero impar de bits. Decodificacao Manchester requer pares de bits.");
    }
    int totalBitsOriginal = totalBitsManchester / 2;
    int tamanhoArrayDecodificado = (totalBitsOriginal + 31) / 32;
    int[] mensagemDecodificada = new int[tamanhoArrayDecodificado];

    // itera os bits em pares
    for (int i = 0; i < totalBitsManchester; i += 2) {

      // le e recupera o bit1 do par
      int bit1 = Auxiliares.lerBits(quadro, i, 1);
      // le e recupera o bit2 do par
      int bit2 = Auxiliares.lerBits(quadro, i + 1, 1);

      // determina qual o bit original
      int bitOriginal;
      if (bit1 == 1 && bit2 == 0) { // 10 -> 1
        bitOriginal = 1;
      } else if (bit1 == 0 && bit2 == 1) { // 01 -> 0
        bitOriginal = 0;
      } else {
        // Par invalido (00 ou 11) detectado! Isso e um erro de codificacao fisica.
        throw new Erro("ERRO NA CAMADA FISICA (MANCHESTER)",
            "Erro de decodificacao Manchester: O par de bits e invalido. Isto indica que um erro de 1 bit no canal corrompeu o sinal. O quadro sera descartado.");
      }

      int posicaoGlobalOriginal = i / 2;
      Auxiliares.escreverBits(mensagemDecodificada, posicaoGlobalOriginal, bitOriginal, 1);

    } // fim for

    return mensagemDecodificada;
  } // fim do metodo

  /**************************************************************
  * Metodo: camadaFisicaReceptoraDecodificacaoManchesterDiferencial
  * Funcao: decodifica Manchester Diferencial (sem transicao -> 1, com transicao -> 0)
  * @param quadro | pacote de bits codificado
  * @return int[] | pacote de inteiros decodificado
  * ********************************************************* */
  public int[] camadaFisicaReceptoraDecodificacaoManchesterDiferencial(int[] quadro) {

    int totalBitsDiferencial = Auxiliares.tratarBits(quadro);
    int totalBitsOriginal = totalBitsDiferencial / 2;
    int tamanhoArrayDecodificado = (totalBitsOriginal + 31) / 32;
    int[] mensagemDecodificada = new int[tamanhoArrayDecodificado];

    int nivelAnterior = 1; // Deve ser igual ao "nivelAtual" inicial do transmissor

    // percorre os bits codificados em pares
    for (int i = 0; i < totalBitsDiferencial; i += 2) {

      int primeiroNivelSinal = Auxiliares.lerBits(quadro, i, 1);

      // determina o bit original: sem transicao (nivelAnterior == primeiroNivelSinal) -> 1
      int bitOriginal;
      if (primeiroNivelSinal == nivelAnterior) { // se o sinal se manteve (sem transicao) logo bit = 1
        bitOriginal = 1;
      } else { // caso contrario (com transicao) logo bit = 0
        bitOriginal = 0;
      }

      int posicaoGlobalOriginal = i / 2;
      Auxiliares.escreverBits(mensagemDecodificada, posicaoGlobalOriginal, bitOriginal, 1);

      // atualiza o nivel anterior com a segunda parte do sinal (que sera o nivel de referencia para o proximo par)
      int nivelAtualSinal2 = Auxiliares.lerBits(quadro, i + 1, 1);
      nivelAnterior = nivelAtualSinal2;

    }

    return mensagemDecodificada;
  } // fim do metodo

  /**************************************************************
  * Metodo: camadaFisicaReceptoraDecodificacaoComViolacao
  * Funcao: decodifica com violacao (retira flags e decodifica dados)
  * @param quadro | o sinal bruto vindo do meio fisico
  * @param tipoDeDecodificacao | a decodificacao a ser utilizada
  * @return int[] | o quadro de dados decodificado e desenquadrado
  * @throws Erro | controla os erros de sinais invalidos
  * ********************************************************* */
  private int[] camadaFisicaReceptoraDecodificacaoComViolacao(int[] quadro, int tipoDeDecodificacao)
      throws Erro {

    final int VIOLACAO = 0b1111;
    final int TAMANHO_VIOLACAO_BITS = 4;

    int totalBitsSinal = Auxiliares.tratarBits(quadro);
    if (totalBitsSinal == 0) return new int[0];

    int[] quadroDecodificado = new int[quadro.length]; // buffer temporario (tamanho maximo, sera ajustado)
    int bitEscritaGlobal = 0;
    boolean quadroIniciado = false;
    int nivelAnterior = 1; // para Manchester Diferencial

    int i = 0; // indice de leitura do quadro codificado

    // Loop principal para decodificar
    while (i <= totalBitsSinal - TAMANHO_VIOLACAO_BITS) {

      // 1. Verifica se há uma VIOLACAO (flag) na posicao atual
      int possivelViolacao = Auxiliares.lerBits(quadro, i, TAMANHO_VIOLACAO_BITS);

      if (possivelViolacao == VIOLACAO) {
        quadroIniciado = true; // Marca o inicio/fim de um subquadro
        i += TAMANHO_VIOLACAO_BITS; // Pula os 4 bits da violacao
        nivelAnterior = 1; // Reseta o nivel para o Manchester Diferencial (como no TX)

        if (i >= totalBitsSinal) break; // Termina se nao ha mais bits
        continue; // Volta ao inicio do loop para processar o que vem depois da flag
      }

      // Se o quadro ainda nao foi iniciado (antes da primeira flag), avanca e ignora
      if (!quadroIniciado) {
        i++;
        continue;
      }

      // Se nao eh violacao, e o quadro foi iniciado, entao estamos lendo dados codificados

      // Garante que ha bits suficientes para um par codificado (2 bits)
      if (i > totalBitsSinal - 2) {
          // Os bits acabaram sem uma flag de violacao para finalizar.
          break;
      }

      // Leitura dos 2 bits do sinal
      int bit1 = Auxiliares.lerBits(quadro, i, 1);
      int bit2 = Auxiliares.lerBits(quadro, i + 1, 1);

      int bitOriginal = 0;

      if (tipoDeDecodificacao == 1) { // Manchester
        if (bit1 == 1 && bit2 == 0) { // 10 -> 1
          bitOriginal = 1;
        } else if (bit1 == 0 && bit2 == 1) { // 01 -> 0
          bitOriginal = 0;
        } else {
          // Erro: Par invalido (00 ou 11)
          throw new Erro("ERRO NA CAMADA FISICA (MANCHESTER/VIOLACAO)",
              "Erro de decodificacao Manchester: O par de bits O quadro sera descartado.");
        }
      } else { // Manchester Diferencial (tipoDeDecodificacao == 2)
        if (bit1 == nivelAnterior) { // sem transicao -> 1
          bitOriginal = 1;
        } else { // com transicao -> 0
          bitOriginal = 0;
        }
        nivelAnterior = bit2; // Atualiza o nivel de referencia para a proxima comparacao
      }

      Auxiliares.escreverBits(quadroDecodificado, bitEscritaGlobal++, bitOriginal, 1);

      i += 2; // Avanca para o proximo par de bits do sinal
    } // fim while

    // 2. Ajusta o array final para o tamanho exato dos bits decodificados
    int tamanhoFinalArray = (bitEscritaGlobal + 31) / 32;
    int[] resultadoFinal = new int[tamanhoFinalArray];

    // Copia apenas os bits uteis
    for (int j = 0; j < bitEscritaGlobal; j++) {
      int bit = Auxiliares.lerBits(quadroDecodificado, j, 1);
      Auxiliares.escreverBits(resultadoFinal, j, bit, 1);
    }
    return resultadoFinal;
  } // fim do metodo CamadaFisicaReceptoraDecodificacaoComViolacao

} // fim da classe