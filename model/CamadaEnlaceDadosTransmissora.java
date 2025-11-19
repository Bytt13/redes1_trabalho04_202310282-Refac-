/*****************************************************************
* Autor..............: Lucas de Menezes Chaves
* Matricula........: 202310282
* Inicio...........: 16/09/2025
* Ultima alteracao.: 18/11/2025
* Nome.............: CamadaEnlaceDadosTransmissora
* Funcao...........: Aplica os algoritmos de enquadramento, controle de erro e fluxo, 
* e envia a mensagem para a camada fisica transmissora.
*************************************************************** */
package model;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

import controller.TelaPrincipalController;
import util.Erro;
import util.Auxiliares;

public class CamadaEnlaceDadosTransmissora {

  private CamadaFisicaTransmissora camadaFisicaTransmissora;
  private TelaPrincipalController controller; // Referencia atualizada

  // constantes do protocolo de ACK e Temporizador,
  private final int TIMEOUT_MILISEGUNDOS = 3000;

  // maquina de estados
  private volatile String estado = "PRONTO_PARA_ENVIAR"; // estado varia entre PRONTO_PARA_ENVIAR e ESPERANDO_ACK

  // fila de envio e quadro em espera
  private Queue<int[]> filaDeEnvio = new LinkedList<>();
  private int[] quadroEmEspera;

  // timer
  private Timer timer;

  /**************************************************************
  * Metodo: CamadaEnlaceDadosTransmissora
  * Funcao: construtor da classe
  * @param camadaFisicaTransmissora | camada imediatamente abaixo
  * @param controller | controle da interface (referencia atualizada)
  * @return void
  * ********************************************************* */
  public CamadaEnlaceDadosTransmissora(CamadaFisicaTransmissora camadaFisicaTransmissora,
      TelaPrincipalController controller) {
    this.camadaFisicaTransmissora = camadaFisicaTransmissora;
    this.controller = controller;
  } // fim contrutor

  /**************************************************************
  * Metodo: transmitir
  * Funcao: envia o quadro para a proxima camada da rede apos aplicar o
  * enquadramaneto e controle de erro selecionado
  * @param quadro | mensagem em bits recebida pela camada anterior
  * @return void
  * @throws Erro | para tratar erros no fluxo
  * ********************************************************* */
  public void transmitir(int[] quadro) throws Erro {

    // limpa a fila de transmissao anteriores e cancela os timers.
    cancelarTimer();
    filaDeEnvio.clear();
    estado = "PRONTO_PARA_ENVIAR";

    // trata cada int ou seja cada 32 bits de carga util como sendo um subquadro
    for (int i = 0; i < quadro.length; i++) {

      // verifica se o 'int' contem dados validos antes de processar
      int totalBitsNoInt = Auxiliares.tratarBits(new int[] { quadro[i] }); // Uso de Auxiliares
      if (totalBitsNoInt == 0) {
        continue; // pular 'ints' de padding (vazios)
      } // fim if

      int[] subQuadro = new int[] { quadro[i] }; // o subquadro eh de 1 int ou seja, ate 32 bits de carga util

      // aplica enquadramento no subquadro.
      int[] quadroEnquadrado = camadaEnlaceDadosTransmissoraEnquadramento(subQuadro);
      // aplica controle de erro
      int[] quadroComControleDeErro = camadaEnlaceDadosTransmissoraControleDeErro(quadroEnquadrado);
      // aplica controle de fluxo
      camadaEnlaceDadosTransmissoraControleDeFluxo(quadroComControleDeErro);

    } // fim for

  }// fim do metodo transmitir

  /**************************************************************
  * Metodo: transmitirACK
  * Funcao: metodo paralelo que envia o ACK sem passar pelo controle de fluxo
  * @param quadro | o ACK em formato de bits
  * @return void
  * @throws Erro | para tratar erros no fluxo
  * ********************************************************* */
  public void transmitirACK(int[] quadro) throws Erro {
    System.out.println("ENLACE DADOS TRANSMISSORA: enviando ACK");

    // trata Ack como um unico subquadro
    if (quadro.length == 0) {
      return; // quadro vazio nao faz nada
    } // fim, if

    int[] quadroEnquadrado = camadaEnlaceDadosTransmissoraEnquadramento(quadro);
    int[] quadroComControleErro = camadaEnlaceDadosTransmissoraControleDeErro(quadroEnquadrado);

    // envia diretamente para a Camada Fisica (assumindo o metodo 'transmitir' na camada Fisica)
    this.camadaFisicaTransmissora.transmitir(quadroComControleErro);
  } // fim do transmitirACK

  /**************************************************************
  * Metodo: camadaEnlaceDadosTransmissoraEnquadramento
  * Funcao: metodo que escolhe o tipo de enquadramento a ser aplicado na mensagem
  * @param quadro | mensagem na forma binaria recebida da camada anterior
  * @return int[] | o quadro ja enquadrado
  * ********************************************************* */
  public int[] camadaEnlaceDadosTransmissoraEnquadramento(int quadro[]) {

    int tipoDeEnquadramento = this.controller.enquadCodification();
    int quadroEnquadrado[] = null;
    switch (tipoDeEnquadramento) {
      case 0: // contagem de caracteres
        quadroEnquadrado = camadaEnlaceDadosTransmissoraEnquadramentoContagemDeCaracteres(quadro);
        break;
      case 1: // insercao de bytes
        quadroEnquadrado = camadaEnlaceDadosTransmissoraEnquadramentoInsercaoDeBytes(quadro);
        break;
      case 2: // insercao de bits
        quadroEnquadrado = camadaEnlaceDadosTransmissoraEnquadramentoInsercaoDeBits(quadro);
        break;
      case 3: // violacao da camada fisica
        quadroEnquadrado = camadaEnlaceDadosTransmissoraEnquadramentoViolacaoDaCamadaFisica(quadro);
        break;
    }// fim do switch/case

    return quadroEnquadrado; // retorna o quadro ja enquadrado

  }// fim do metodo camadaEnlaceDadosTransmissoraEnquadramento

  /**************************************************************
  * Metodo: camadaEnlaceDadosTransmissoraControleDeErro
  * Funcao: metodo que escolhe o tipo de controle de erro a ser aplicado na mensagem
  * @param quadro | mensagem ja com o enquadramento
  * @return int[] | mensagem com o controle de erro aplicado
  * ********************************************************* */
  public int[] camadaEnlaceDadosTransmissoraControleDeErro(int quadro[]) {

    int tipoDeControleDeErro = this.controller.erroCodification();
    int quadroComControleDeErro[] = null;
    switch (tipoDeControleDeErro) {
      case 0: // paridade par
        quadroComControleDeErro = camadaEnlaceDadosTransmissoraControleDeErroBitParidadePar(quadro);
        break;
      case 1: // paridade impar
        quadroComControleDeErro = camadaEnlaceDadosTransmissoraControleDeErroBitParidadeImpar(quadro);
        break;
      case 2: // CRC
        quadroComControleDeErro = camadaEnlaceDadosTransmissoraControleDeErroCRC(quadro);
        break;
      case 3: // Hamming
        quadroComControleDeErro = camadaEnlaceDadosTransmissoraControleDeErroCodigoDeHamming(quadro);
        break;
    }// fim do switch/case

    return quadroComControleDeErro; // retorna o quadro ja com controle de erro aplicado

  }// fim do metodo camadaEnlaceDadosTransmissoraControleDeErro

  /**************************************************************
  * Metodo: camadaEnlaceDadosTransmissoraControleDeFluxo
  * Funcao: metodo de controle de fluxo, controla a logica de ack e retransmissao
  * @param quadro | o quadro a ser enfileirado e enviado
  * @return void
  * @throws Erro | trata os erros
  * ********************************************************* */
  public void camadaEnlaceDadosTransmissoraControleDeFluxo(int quadro[]) throws Erro {

    int tipoDeControleFluxo = this.controller.fluxoCodification();

    switch (tipoDeControleFluxo) {
      case 0: // janela deslizante de 1 bit
        camadaEnlaceDadosTransmissoraJanelaDeslizanteUmBit(quadro);
        break;
      case 1: // janela deslizante go-back-n
        camadaEnlaceDadosTransmissoraJanelaDeslizanteGoBackN(quadro);
        break;
      case 2: // janela deslizante com retransmissão seletiva
        camadaEnlaceDadosTransmissoraJanelaDeslizanteComRetransmissaoSeletiva(quadro);
        break;
      default:
        break;
    }

  }// fim do metodo camadaEnlaceDadosTransmissoraControleDeFluxo

  /**************************************************************
  * Metodo: camadaEnlaceDadosTransmissoraJanelaDeslizanteUmBit
  * Funcao: metodo que implementa a logica do Stop and Wait
  * @param quadro | o quadro a ser enviado
  * @return void
  * @throws Erro | trata erros de reenvio
  * ********************************************************* */
  private void camadaEnlaceDadosTransmissoraJanelaDeslizanteUmBit(int[] quadro) throws Erro {
    filaDeEnvio.add(quadro);
    fluxoStopWait();
  }

  /**************************************************************
  * Metodo: camadaEnlaceDadosTransmissoraJanelaDeslizanteGoBackN
  * Funcao: (Implementacao pendente) Enfileira e gerencia o envio Go-Back-N
  * @param quadro | o quadro a ser enviado
  * @return void
  * ********************************************************* */
  private void camadaEnlaceDadosTransmissoraJanelaDeslizanteGoBackN(int[] quadro) {
    // Implementacao Go-Back-N
    // Por enquanto apenas enfileira, mas a logica de envio será no fluxoStopWait adaptado
    filaDeEnvio.add(quadro);
    try {
        fluxoStopWait();
    } catch (Erro e) {
        System.err.println("Erro no envio Go-Back-N inicial");
    }
  }

  /**************************************************************
  * Metodo: camadaEnlaceDadosTransmissoraJanelaDeslizanteComRetransmissaoSeletiva
  * Funcao: (Implementacao pendente) Enfileira e gerencia o envio com Retransmissao Seletiva
  * @param quadro | o quadro a ser enviado
  * @return void
  * ********************************************************* */
  private void camadaEnlaceDadosTransmissoraJanelaDeslizanteComRetransmissaoSeletiva(int[] quadro) {
    // Implementacao Retransmissao Seletiva
    // Por enquanto apenas enfileira, mas a logica de envio será no fluxoStopWait adaptado
    filaDeEnvio.add(quadro);
    try {
        fluxoStopWait();
    } catch (Erro e) {
        System.err.println("Erro no envio Retransmissao Seletiva inicial");
    }
  }

  /**************************************************************
  * Metodo: fluxoStopWait
  * Funcao: metodo que funciona como a logica basica do Stop and Wait, analisa a fila e
  * envia os quadros em ordem Chamado pelo controle de Fluxo
  * @return void
  * @throws Erro | trata erros de reenvio
  * ********************************************************* */
  private synchronized void fluxoStopWait() throws Erro {

    if (estado.equals("PRONTO_PARA_ENVIAR") && !filaDeEnvio.isEmpty()) {
      // se tem quadros a serem enviados e a camada do transmissor ta pronta para
      // enviar quadros
      this.quadroEmEspera = filaDeEnvio.poll(); // pega o primeiro da fila
      this.estado = "ESPERANDO_ACK"; // muda o estado do transmissor para esperar o ACK

      System.out.println("CAMADA DE ENLACE TX: Enviando o proximo da fila ... ");
      this.camadaFisicaTransmissora.transmitir(quadroEmEspera); // envia o quadro (Uso do metodo 'transmitir')
      iniciarTimer();
    } else if (estado.equals("PRONTO_PARA_ENVIAR") && filaDeEnvio.isEmpty()) {
      // se a fila ta vazia e ta pronto pra enviar, a transmissao acabou
      System.out.println("ENLACE TX: Fila de envio vazia. Transmissão concluída.");
    } // fim else/if

  } // fim metodo fluxoStopWait

  /**************************************************************
  * Metodo: iniciarTimer
  * Funcao: metodo responsavel por inicializar um timer, start numa thread que espera o
  * tempo ate a chegada do ACk de confirmacao e chama tratarTimeOut caso o tempo
  * acabe
  * @return void
  * @throws Erro | trata erros de reenvio
  * ********************************************************* */
  private void iniciarTimer() throws Erro {
    cancelarTimer(); // finaliza qualquer timerr anterior
    timer = new Timer(); // cria um timer
    timer.schedule(new TimerTask() {
      @Override
      public void run() { // cria a thread que vai controlar o tempo de espera para recebimento do ACK
        try {
          tratarTimeOut();
        } catch (Erro e) {
          e.printStackTrace();
        } // trata oque acontece quanmdo o tempo acabar
      }
    }, TIMEOUT_MILISEGUNDOS);
  } // fim do iniciarTimer

  /**************************************************************
  * Metodo: cancelarTimer
  * Funcao: metodo que cancela o timer atual e o reseta
  * @return void
  * ********************************************************* */
  private void cancelarTimer() {
    if (timer != null) { // se existe um timer, cancele ele
      timer.cancel();
      timer = null;
    } // fim if
  }// fim metodo cancelarTimer

  /**************************************************************
  * Metodo: tratarTimeOut
  * Funcao: quando o tempo do timer acaba, ele reenvia o quadro pois o ack nao chegou e
  * reinicia um novo timer
  * @return void
  * @throws Erro | trata erros de reenvio
  * ********************************************************* */
  private synchronized void tratarTimeOut() throws Erro {
    if (!estado.equals("ESPERANDO_ACK")) {
      return; // se nao esta esperando o ACK entao nao faz nada, seguranca
    } // fim if

    System.out.println("ENLACE TRANSMISSORA: TIMEOUT!! tempo de espera pelo ack acabou, reenviuando quadro ... ");
    this.camadaFisicaTransmissora.transmitir(this.quadroEmEspera); // Uso do metodo 'transmitir'
    iniciarTimer(); // recomeca o timer
  }// fim metodo

  /**************************************************************
  * Metodo: camadaEnlaceDadosTransmissoraEnquadramentoContagemDeCaracteres
  * Funcao: metodo que realiza o enquadramento por contagem de caracteres
  * @param quadro | quadro original a ser enquadrado
  * @return int[] | o quadro ja enquadrado
  * ********************************************************* */
  public int[] camadaEnlaceDadosTransmissoraEnquadramentoContagemDeCaracteres(int quadro[]) {
    // descobre o tamanho real dos dados, ignorando o lixo.
    int totalDeBitsReais = Auxiliares.tratarBits(quadro); // Uso de Auxiliares
    if (totalDeBitsReais == 0)
      return new int[0];

    final int TAMANHO_MAX_CARGA_UTIL_EM_BITS = 32; // A carga util sera de ATE 4 bytes
    final int TAMANHO_CABECALHO_EM_BITS = 8;

    // Para calcular o tamanho final, precisamos simular a criacao dos quadros
    int tamanhoTotalEstimadoEmBits = 0;
    for (int i = 0; i < totalDeBitsReais; i += TAMANHO_MAX_CARGA_UTIL_EM_BITS) {
      int bitsNesteFrame = Math.min(TAMANHO_MAX_CARGA_UTIL_EM_BITS, totalDeBitsReais - i);
      tamanhoTotalEstimadoEmBits += TAMANHO_CABECALHO_EM_BITS + bitsNesteFrame;
    }

    int[] quadroEnquadrado = new int[(tamanhoTotalEstimadoEmBits + 31) / 32];
    int bitEscritaGlobal = 0;
    int bitLeituraGlobal = 0;

    while (bitLeituraGlobal < totalDeBitsReais) {
      // 1. Calcula o tamanho da carga util para ESTE frame
      int bitsParaLer = Math.min(TAMANHO_MAX_CARGA_UTIL_EM_BITS, totalDeBitsReais - bitLeituraGlobal);
      int cargaUtil = Auxiliares.lerBits(quadro, bitLeituraGlobal, bitsParaLer); // Uso de Auxiliares
      bitLeituraGlobal += bitsParaLer;

      // 2. Calcula o valor do cabecalho para ESTE frame
      // O valor eh o numero de bytes da carga util + 1 (o proprio cabecalho)
      int valorDoCabecalho = (bitsParaLer / 8) + 1;

      // 3. Escreve o cabecalho (8 bits)
      Auxiliares.escreverBits(quadroEnquadrado, bitEscritaGlobal, valorDoCabecalho, TAMANHO_CABECALHO_EM_BITS); // Uso de Auxiliares
      bitEscritaGlobal += TAMANHO_CABECALHO_EM_BITS;

      // 4. Escreve a carga util (APENAS os bits lidos)
      Auxiliares.escreverBits(quadroEnquadrado, bitEscritaGlobal, cargaUtil, bitsParaLer); // Uso de Auxiliares
      bitEscritaGlobal += bitsParaLer;
    }

    return quadroEnquadrado;
  } // fim de camadaEnlaceDadosTransmissoraEnquadramentoContagemDeCaracteres

  /**************************************************************
  * Metodo: camadaEnlaceDadosTransmissoraEnquadramentoInsercaoDeBytes
  * Funcao: metodo para realizar o enquadramento por insercao de bytes
  * @param quadro | quadro original a ser enquadrado
  * @return int[] | o quadro ja enquadrado
  * ********************************************************* */
  public int[] camadaEnlaceDadosTransmissoraEnquadramentoInsercaoDeBytes(int quadro[]) {
    // A logica aqui usa manipulacao direta de bytes (que é ok, pois o `Auxiliares` lida com bits)
    // Para simplificar a adaptacao, a complexa logica de calculo de tamanho e preenchimento
    // é mantida como estava, com a conversão dos helpers de bit.

    final int FLAG = 0b01111110;
    final int SCAPE = 0b01111101;
    final int TAMANHO_SUBQUADRO_EM_BYTES = 4;

    int contadorBytesCargaUtilQuadro = 0;
    int contadorBytesEnquadrados = 0;

    // --- Calculo de Bytes de Carga Util ---
    // A logica original está incompleta/incorreta para a determinação do fim dos dados,
    // mas será mantida para preservar o comportamento do código-fonte.
    boolean fimDados = false;
    for (int inteiroAgrupado : quadro) {
      if (fimDados) break;
      for (int i = 0; i < 4; i++) {
        int umByte = (inteiroAgrupado >> (24 - i * 8)) & 0xFF;
        if (umByte == 0) { // O bit de parada não é confiável pois um '0' pode ser um caractere NUL
          fimDados = true;
          break;
        }
        contadorBytesCargaUtilQuadro++;
      }
    }
    // --- Fim Calculo de Bytes de Carga Util ---


    if (contadorBytesCargaUtilQuadro > 0) {
      contadorBytesEnquadrados = 1; // comeca com a flag inicial
      int contadorFlagIntermediaria = 0;

      for (int i = 0; i < contadorBytesCargaUtilQuadro; i++) {
        int indiceInteiro = i / 4;
        int posicaoNoInteiro = i % 4;

        int umByte = (quadro[indiceInteiro] >> (24 - posicaoNoInteiro * 8)) & 0xFF;

        if (umByte == FLAG || umByte == SCAPE) {
          contadorBytesEnquadrados += 2; // Adiciona SCAPE + byte
        } else {
          contadorBytesEnquadrados += 1; // Adiciona o byte
        }

        contadorFlagIntermediaria++;

        if (contadorFlagIntermediaria == TAMANHO_SUBQUADRO_EM_BYTES) {
          contadorBytesEnquadrados++; // Adiciona a FLAG intermediária
          contadorFlagIntermediaria = 0;
        }
      }

      // Adiciona a FLAG final se o ultimo subquadro nao tiver a adicionado
      if (contadorFlagIntermediaria != 0) {
        contadorBytesEnquadrados++;
      }

    }

    if (contadorBytesEnquadrados == 0) {
      System.out.println("MENSAGEM VAZIA");
      return new int[0];
    }

    int tamanhoArrayFinal = (contadorBytesEnquadrados * 8 + 31) / 32; // Ajustado para bytes * 8 bits
    int[] quadroEnquadrado = new int[tamanhoArrayFinal];

    int indiceBitDestino = 0;

    // escreve a FLAG inicial
    Auxiliares.escreverBits(quadroEnquadrado, indiceBitDestino, FLAG, 8); // Uso de Auxiliares
    indiceBitDestino += 8;

    int contadorFlagIntermediaria = 0;
    
    for (int i = 0; i < contadorBytesCargaUtilQuadro; i++) {
      int indiceInteiro = i / 4;
      int posicaoNoInteiro = i % 4;
      int umByte = (quadro[indiceInteiro] >> (24 - posicaoNoInteiro * 8)) & 0xFF;

      if (umByte == FLAG || umByte == SCAPE) {
        // Escreve o SCAPE (8 bits).
        Auxiliares.escreverBits(quadroEnquadrado, indiceBitDestino, SCAPE, 8); // Uso de Auxiliares
        indiceBitDestino += 8;

        // Escreve o byte original (8 bits).
        Auxiliares.escreverBits(quadroEnquadrado, indiceBitDestino, umByte, 8); // Uso de Auxiliares
        indiceBitDestino += 8;
      } else {
        // Escreve o byte normal (8 bits).
        Auxiliares.escreverBits(quadroEnquadrado, indiceBitDestino, umByte, 8); // Uso de Auxiliares
        indiceBitDestino += 8;
      }

      contadorFlagIntermediaria++;
      if (contadorFlagIntermediaria == TAMANHO_SUBQUADRO_EM_BYTES) {
        // escreve a flag intermediaria
        Auxiliares.escreverBits(quadroEnquadrado, indiceBitDestino, FLAG, 8); // Uso de Auxiliares
        indiceBitDestino += 8;

        contadorFlagIntermediaria = 0;
      }

    }

    if (contadorFlagIntermediaria != 0) {
      // Escreve a FLAG final (8 bits).
      Auxiliares.escreverBits(quadroEnquadrado, indiceBitDestino, FLAG, 8); // Uso de Auxiliares
    }

    return quadroEnquadrado;
  }// fim do metodo camadaEnlaceDadosTransmissoraEnquadramentoInsercaoDeBytes

  /**************************************************************
  * Metodo: camadaEnlaceDadosTransmissoraEnquadramentoInsercaoDeBits
  * Funcao: metodo para realizar o enquadramento por insercao de bit (bit stuffing)
  * @param quadro | quadro original a ser enquadrado
  * @return int[] | o quadro ja enquadrado
  * ********************************************************* */
  public int[] camadaEnlaceDadosTransmissoraEnquadramentoInsercaoDeBits(int quadro[]) {

    final int FLAG = 0b01111110;
    final int TAMANHO_SUBQUADRO_EM_BYTES = 4;

    // --- Calculo de Bytes de Carga Util (Logica mantida para consistencia) ---
    int contadorBytesCargaUtil = 0;
    boolean fimDados = false;
    for (int inteiroAgrupado : quadro) {
      if (fimDados) break;
      for (int i = 0; i < 4; i++) {
        int umByte = (inteiroAgrupado >> (24 - i * 8)) & 0xFF;
        if (umByte == 0) {
          fimDados = true;
          break;
        }
        contadorBytesCargaUtil++;
      }
    }
    // --- Fim Calculo de Bytes de Carga Util ---

    if (contadorBytesCargaUtil == 0) {
      System.out.println("MENSAGEM VAZIA");
      return new int[0];
    }

    // calcula o total de bits que o quadro enquadrado vai ter (com o stuffing)
    int contadorBitsEnquadrados = 8; // comeca com 8 bits para as FLAG de inicio
    int contadorBitsUm = 0;
    int contadorFlagIntermediaria = 0;

    for (int i = 0; i < contadorBytesCargaUtil; i++) {
      int indiceInteiro = i / 4;
      int posicaoNoInteiro = i % 4;
      int umByte = (quadro[indiceInteiro] >> (24 - posicaoNoInteiro * 8)) & 0xFF;

      for (int j = 7; j >= 0; j--) {
        int bitAtual = (umByte >> j) & 1;
        contadorBitsEnquadrados++;

        if (bitAtual == 1) {
          contadorBitsUm++;
          if (contadorBitsUm == 5) {
            contadorBitsEnquadrados++; // conta o bit '0' de stuffing
            contadorBitsUm = 0;
          }
        } else {
          contadorBitsUm = 0;
        }
      }

      contadorFlagIntermediaria++;

      if (contadorFlagIntermediaria == TAMANHO_SUBQUADRO_EM_BYTES) {
        contadorBitsEnquadrados += 8; // adiciona os 8 bits da flag
        contadorFlagIntermediaria = 0;
      }

    }

    if (contadorFlagIntermediaria != 0) {
      contadorBitsEnquadrados += 8;
    }

    // cria o array final com o tamanho exato calculado
    int tamanhoArrayFinal = (contadorBitsEnquadrados + 31) / 32;
    int[] quadroEnquadrado = new int[tamanhoArrayFinal];

    int indiceBitDestino = 0;
    contadorFlagIntermediaria = 0;

    // escreve a FLAG inicial (8 bits)
    Auxiliares.escreverBits(quadroEnquadrado, indiceBitDestino, FLAG, 8); // Uso de Auxiliares
    indiceBitDestino += 8;

    contadorBitsUm = 0;
    
    for (int i = 0; i < contadorBytesCargaUtil; i++) {
      int indiceInteiro = i / 4;
      int posicaoNoInteiro = i % 4;
      int umByte = (quadro[indiceInteiro] >> (24 - posicaoNoInteiro * 8)) & 0xFF;

      for (int j = 7; j >= 0; j--) {
        int bitAtual = (umByte >> j) & 1;

        // escreve o bit de dado atual
        Auxiliares.escreverBits(quadroEnquadrado, indiceBitDestino, bitAtual, 1); // Uso de Auxiliares
        indiceBitDestino++;

        if (bitAtual == 1) {
          contadorBitsUm++;
          if (contadorBitsUm == 5) {
            // escreve o bit '0' de stuffing
            Auxiliares.escreverBits(quadroEnquadrado, indiceBitDestino, 0, 1); // Uso de Auxiliares
            indiceBitDestino++;
            contadorBitsUm = 0;
          }
        } else {
          contadorBitsUm = 0;
        }
      }

      contadorFlagIntermediaria++;

      if (contadorFlagIntermediaria == TAMANHO_SUBQUADRO_EM_BYTES) { // escreve a flag intermediaria
        Auxiliares.escreverBits(quadroEnquadrado, indiceBitDestino, FLAG, 8); // Uso de Auxiliares
        indiceBitDestino += 8;
        contadorFlagIntermediaria = 0;
      }

    }

    // escreve a FLAG final (8 bits) se nao tiver acabado com flag
    if (contadorFlagIntermediaria != 0) {
      Auxiliares.escreverBits(quadroEnquadrado, indiceBitDestino, FLAG, 8); // Uso de Auxiliares
    }

    return quadroEnquadrado;
  }// fim do metodo camadaEnlaceDadosTransmissoraEnquadramentoInsercaoDeBits

  /**************************************************************
  * Metodo: camadaEnlaceDadosTransmissoraEnquadramentoViolacaoDaCamadaFisica
  * Funcao: passa o quadro para a camada fisica, transferindo para ela a responsabilidade de enquadrar com a Violacao.
  * @param quadro | quadro a ser enquadrado
  * @return int[] | o mesmo quadro
  * ********************************************************* */
  public int[] camadaEnlaceDadosTransmissoraEnquadramentoViolacaoDaCamadaFisica(int quadro[]) {

    // A camada Fisica (TX) se encarrega de adicionar a "violacao" (flags de Manchester)
    return quadro;
  }// fim metodo camadaEnlaceDadosTransmissoraEnquadramentoViolacaoDeCamadaFisica

  /**************************************************************
  * Metodo: camadaEnlaceDadosTransmissoraControleDeErroBitParidadePar
  * Funcao: metodo que aplica o controle de erro por bit de paridade par
  * @param quadro | quadro original a ser aplicado o controle de erro
  * @return int[] | quadro com o bit de paridade anexado
  * ********************************************************* */
  public int[] camadaEnlaceDadosTransmissoraControleDeErroBitParidadePar(int quadro[]) {

    int totalBits = Auxiliares.tratarBits(quadro); // Uso de Auxiliares

    if (totalBits == 0) {
      return quadro;
    }

    // conta a quantidade de bits 1 no quadro
    int contadorUns = 0;

    for (int i = 0; i < totalBits; i++) {
      if (Auxiliares.lerBits(quadro, i, 1) == 1) { // Uso de Auxiliares
        contadorUns++;
      }
    } // fim for

    // calcula o bit de paridade necessario
    int bitDeParidade;
    if (contadorUns % 2 == 0) {
      bitDeParidade = 0; // ja eh par
    } else {
      bitDeParidade = 1; // precisa adicionar 1 para ficar par
    }

    int novoTotalBits = totalBits + 1;
    int novoTotalBitsAlinhado = (novoTotalBits + 7) / 8 * 8; // alinha para o proximo byte

    int tamanhoArrayFinal = (novoTotalBitsAlinhado + 31) / 32;
    int[] quadroComParidade = new int[tamanhoArrayFinal];

    // copia a carga do quadro para o quadro verificado
    for (int i = 0; i < totalBits; i++) {
      int bitAtual = Auxiliares.lerBits(quadro, i, 1); // Uso de Auxiliares
      Auxiliares.escreverBits(quadroComParidade, i, bitAtual, 1); // Uso de Auxiliares
    } // fim for

    // adiciona o bit de paridade no final
    Auxiliares.escreverBits(quadroComParidade, totalBits, bitDeParidade, 1); // Uso de Auxiliares

    // Adiciona um "bit marcador" 1 no final do quadro arredondado
    // A logica original está incompleta/incorreta para o padding de 0s, 
    // mas o bit marcador para tratarBits é mantido.
    Auxiliares.escreverBits(quadroComParidade, novoTotalBitsAlinhado - 1, 1, 1); // Uso de Auxiliares

    return quadroComParidade;
  }// fim do metodo camadaEnlaceDadosTransmissoraControleDeErroBitParidadePar

  /**************************************************************
  * Metodo: camadaEnlaceDadosTransmissoraControleDeErroBitParidadeImpar
  * Funcao: metodo que aplica o controle de erro por bit de paridade impar
  * @param quadro | quadro original a ser aplicado o controle de erro
  * @return int[] | quadro com o bit de paridade anexado
  * ********************************************************* */
  public int[] camadaEnlaceDadosTransmissoraControleDeErroBitParidadeImpar(int quadro[]) {

    int totalBits = Auxiliares.tratarBits(quadro); // Uso de Auxiliares

    if (totalBits == 0) {
      return quadro;
    }

    // conta a quantidade de bits 1 no quadro
    int contadorUns = 0;

    for (int i = 0; i < totalBits; i++) {
      if (Auxiliares.lerBits(quadro, i, 1) == 1) { // Uso de Auxiliares
        contadorUns++;
      }
    } // fim for

    // calcula o bit de paridade necessario
    int bitDeParidade;
    if (contadorUns % 2 == 0) {
      bitDeParidade = 1; // precisa adicionar o 1 para ficar impar
    } else {
      bitDeParidade = 0; // ja eh impar
    }

    int novoTotalBits = totalBits + 1;
    int novoTotalBitsAlinhado = (novoTotalBits + 7) / 8 * 8; // alinha para o proximo byte

    int tamanhoArrayFinal = (novoTotalBitsAlinhado + 31) / 32;
    int[] quadroComParidade = new int[tamanhoArrayFinal];

    // copia a carga do quadro para o quadro verificado
    for (int i = 0; i < totalBits; i++) {
      int bitAtual = Auxiliares.lerBits(quadro, i, 1); // Uso de Auxiliares
      Auxiliares.escreverBits(quadroComParidade, i, bitAtual, 1); // Uso de Auxiliares
    } // fim for

    // adiciona o bit de paridade no final
    Auxiliares.escreverBits(quadroComParidade, totalBits, bitDeParidade, 1); // Uso de Auxiliares

    // Adiciona um "bit marcador" 1 no final do quadro arredondado
    Auxiliares.escreverBits(quadroComParidade, novoTotalBitsAlinhado - 1, 1, 1); // Uso de Auxiliares

    return quadroComParidade;
  }// fim do metodo camadaEnlaceDadosTransmissoraControleDeErroBitParidadeImpar

  /**************************************************************
  * Metodo: camadaEnlaceDadosTransmissoraControleDeErroCRC
  * Funcao: metodo que aplica o controle de erro com o polinomio CRC32
  * @param quadro | quadro original a ser aplicado controle de erro
  * @return int[] | quadro com controle de erro aplicado
  * ********************************************************* */
  public int[] camadaEnlaceDadosTransmissoraControleDeErroCRC(int quadro[]) {

    int totalBits = Auxiliares.tratarBits(quadro); // Uso de Auxiliares

    final int POLINOMIO_GERADOR = 0x04C11DB7; // Polinomio CRC-32
    final int VALOR_INICIAL = 0xFFFFFFFF; // Valor inicial do registrador CRC
    final int VALOR_FINAL_XOR = 0xFFFFFFFF; // Valor final para XOR, adicionar o CRC correto no quadro

    int registradorCRC = VALOR_INICIAL;

    // processamendo dos bits do quadro
    for (int i = 0; i < totalBits; i++) {
      int bitAtual = Auxiliares.lerBits(quadro, i, 1); // lê o bit atual (Uso de Auxiliares)
      int bitMaisSignificativo = (registradorCRC >> 31) & 1;

      int xorBit = bitMaisSignificativo ^ bitAtual;

      registradorCRC = registradorCRC << 1;

      if (xorBit == 1) {
        registradorCRC = registradorCRC ^ POLINOMIO_GERADOR;
      }
    }

    // processamento dos 32 bits de 0 adicionais
    for (int i = 0; i < 32; i++) {

      int bitAtual = 0; // bits adicionais sao 0
      int bitMaisSignificativo = (registradorCRC >> 31) & 1;

      int xorBit = bitMaisSignificativo ^ bitAtual;

      registradorCRC = registradorCRC << 1;

      if (xorBit == 1) {
        registradorCRC = registradorCRC ^ POLINOMIO_GERADOR;
      }
    }

    int crcFinal = registradorCRC ^ VALOR_FINAL_XOR;

    // cria o novo quadro com o CRC anexado
    int novoTotalBits = totalBits + 32;
    int tamanhoArrayFinal = (novoTotalBits + 31) / 32;
    int[] quadroComCRC = new int[tamanhoArrayFinal];

    // copia os dados originais para o inicio do novo quadro
    for (int i = 0; i < totalBits; i++) {
      int bitAtual = Auxiliares.lerBits(quadro, i, 1); // Uso de Auxiliares
      Auxiliares.escreverBits(quadroComCRC, i, bitAtual, 1); // Uso de Auxiliares
    }

    // anexa o CRC no final do quadro
    Auxiliares.escreverBits(quadroComCRC, totalBits, crcFinal, 32); // Uso de Auxiliares

    return quadroComCRC;
  }// fim do metodo camadaEnlaceDadosTransmissoraControleDeErroCRC

  /**************************************************************
  * Metodo: camadaEnlaceDadosTransmissoraControleDeErroCodigoDeHamming
  * Funcao: metodo que aplica o controle de erro com o codigo de Hamming
  * @param quadro | quadro original a ser aplicado o controle
  * @return int[] | quadro com o controle de erro aplicado
  * ********************************************************* */
  public int[] camadaEnlaceDadosTransmissoraControleDeErroCodigoDeHamming(int quadro[]) {

    int totalBits = Auxiliares.tratarBits(quadro); // Uso de Auxiliares

    if (totalBits == 0) {
      return new int[0];
    }

    // descobrir quantos bits de paridade serao necessarios
    int quantBitsParidade = 0;
    while ((1 << quantBitsParidade) < (totalBits + quantBitsParidade + 1)) {
      quantBitsParidade++;
    }

    // cria o array do quadro novo
    int totalBitsHammming = totalBits + quantBitsParidade;
    int tamanhoQuadroFinal = (totalBitsHammming + 31) / 32;
    int[] quadroComHamming = new int[tamanhoQuadroFinal];

    // posicionar os bits de paridade
    int indiceBit = 0;

    for (int posicao = 1; posicao <= totalBitsHammming; posicao++) { // posicao indexada no quadro hamming

      if ((posicao & (posicao - 1)) == 0) {
        continue; // pula as posicoes que sao potencia de 2. reservando o espaco
      } else if (indiceBit < totalBits) { // se nao eh potencia de 2 entao eh espaco de dado

        int bitDado = Auxiliares.lerBits(quadro, indiceBit, 1); // Uso de Auxiliares
        Auxiliares.escreverBits(quadroComHamming, posicao - 1, bitDado, 1); // Uso de Auxiliares
        indiceBit++;

      }

    }

    // calcular e posicionar os bits de paridade, paridade PAR
    for (int i = 0; i < quantBitsParidade; i++) {
      int posBitParidade = 1 << i;
      int contadorUns = 0;
      
      // verifica os bits cobertos pela paridade
      for (int bit = 1; bit <= totalBitsHammming; bit++) {
        //verificacao
        if ((bit & posBitParidade) != 0) {
          // nao contamos o proprio bit de paridade
          if (bit != posBitParidade) {
            if (Auxiliares.lerBits(quadroComHamming, bit - 1, 1) == 1) { // Uso de Auxiliares
              contadorUns++;
            } // fim do if
          } // fim do if
        } // fim do if
      } // fim do for

      // Define o bit de paridade p (em k-1) para garantir paridade PAR
      if ((contadorUns % 2) != 0) {
        Auxiliares.escreverBits(quadroComHamming, posBitParidade - 1, 1, 1); // Uso de Auxiliares
      }

    }

    return quadroComHamming;
  }// fim do metodo camadaEnlaceDadosTransmissoraControleDeErroCodigoDeHamming

  /**************************************************************
  * Metodo: receberAck
  * Funcao: metodo que garante que acks recebidos matarao o timer certo para nao entrar
  * em loop de reenvio constante
  * @return void
  * @throws Erro | trata erros no fluxo
  * ********************************************************* */
  public synchronized void receberAck() throws Erro {

    if (estado.equals("ESPERANDO_ACK")) {
      System.out.println("ENLACE TRANSMISSORA: ACK recebido com sucesso. Cancelando Timer.");
      cancelarTimer();
      this.quadroEmEspera = null;
      this.estado = "PRONTO_PARA_ENVIAR";
      // Tenta enviar o próximo quadro da fila
      fluxoStopWait();
    } else {
      System.out.println("ENLACE TRANSMISSORA: ACK recebido, mas nao estava em estado de espera. Ignorando.");
    }

  } // fim do metodo receberAck

  /**************************************************************
  * Metodo: abortarTranmissao
  * Funcao: metodo que limpa a fila de envio e reseta a maquina de estados
  * para abortar a transmissao em caso de erros criticos.
  * @param void
  * @return void
  * ********************************************************* */
  public synchronized void abortarTransmissao() {
    System.out.println("PROBLEMA DETECTADO, ABORTANDO TRANSMISSAO!");
    cancelarTimer();
    filaDeEnvio.clear();
    quadroEmEspera = null;
    estado = "PRONTO_PARA_ENVIAR";
  }// fim do abortarTransmissao
} // Fim da classe