/***************************************************************** 
* Autor............: Lucas de Menezes Chaves
* Matricula........: 202310282
* Inicio...........: 18/08/2025
* Ultima alteracao.: 17/11/2025
* Nome.............: TelaPrincipalController
* Funcao...........: Faz a mediacao entre codigo e GUI, controlando
* o que deve ser feito quando acontecer alguma acao na interface.
*************************************************************** */
package controller;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.paint.Color;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

// Imports da nova arquitetura
import util.Erro;
import util.Auxiliares; 

@SuppressWarnings("unused")
public class TelaPrincipalController {

  // Variaveis FXML do arquivo 'TelaPrincipalRedes.fxml'
  @FXML
  private Button botaoEnviar;

  @FXML
  private TextArea textAreaMensagemOriginal;

  @FXML
  private TextArea textAreaMensagemFinal;

  @FXML
  private TextArea textAreaCodificada;

  @FXML
  private TextArea textAreaDecodificada;

  @FXML
  private ComboBox<String> comboBoxCodificacao;

  @FXML
  private ComboBox<String> comboBoxEnquadramento;

  @FXML
  private ComboBox<String> comboBoxErro;

  @FXML
  private ComboBox<String> comboBoxControleErro;

  @FXML
  private ComboBox<String> comboBoxFluxo;

  @FXML
  private Canvas canvasAnimacao;

  // Variaveis de controle da animacao
  private GraphicsContext gc; // o "pincel" que vai gerar o desenho para simular a transmissao
  private AnimationTimer animacao; // o controle do loop da animacao
  private Rede controleRede; // o controlador da rede

  /***************************************************************** 
  * Autor............: Lucas de Menezes Chaves
  * Matricula........: 202310282
  * Inicio...........: 17/11/2025
  * Ultima alteracao.: 17/11/2025
  * Nome.............: QuadroAnimacao
  * Funcao...........: Classe interna para cuidar da animacao dos quadros
  * ************************************************************** */
  private class QuadroAnimacao {
    int[] fluxoBits;
    QuadroAnimacao proxQuadroAnimacao;

    QuadroAnimacao(int[] fluxoBits) {
      this.fluxoBits = fluxoBits;
      this.proxQuadroAnimacao = null;
    }// fim construtor
  } // fim classe QuadroAnimacao

  // Fila de animacao
  private QuadroAnimacao inicioFilaAnimacao = null;
  private QuadroAnimacao fimFilaAnimacao = null;
  private boolean animacaoEmAndamento = false;

  /****************************************************************
  * Metodo: initialize
  * Funcao: carrega os elementos fxml para tela
  * @param void
  * @return void 
  * ********************************************************* */
  @FXML
  public void initialize() {

    gc = canvasAnimacao.getGraphicsContext2D(); // pega o pincel que vamos utilizar no canvas
    comboBoxCodificacao.getItems().addAll("Binario", "Manchester", "Manchester Diferencial"); //adiciona os elementos ao combo box
    comboBoxCodificacao.getSelectionModel().selectFirst(); // Deixa o primeiro item ja selecionado
    comboBoxEnquadramento.getItems().addAll("Contagem de Caracteres", "Insercao de bytes", "Insercao de bits", "Violacao da Camada Fisica"); // adiciona os elementos ao combo box
    comboBoxEnquadramento.getSelectionModel().selectFirst(); // Deixa o primeiro item ja selecionado
    comboBoxErro.getItems().addAll("0%","10%","20%","30%","40%","50%","60%","70%","80%","90%","100%"); // adiciona os elementos ao combo box
    comboBoxErro.getSelectionModel().selectFirst(); // Deixa o primeiro item ja selecionado
    comboBoxControleErro.getItems().addAll("Bit de Paridade par", "Bit de paridade impar", "CRC", "Codigo de Hamming"); // adiciona as opcoes ao combobox
    comboBoxControleErro.getSelectionModel().selectFirst(); // deixa o primeiro item ja selecionado
    comboBoxFluxo.getItems().addAll("Janela deslizante 1 bit", "Go Back N", "Retransmissão Seletiva"); // adiciona as opcoes a combobox
    comboBoxFluxo.getSelectionModel().selectFirst(); // deixa o primeiro item já selecionado

    // Inicializa a nova arquitetura de Rede
    this.controleRede = new Rede(this); // Passa este controller para a 'Rede'
  } // fim initialize

  /**************************************************************
  * Metodo: enviar
  * Funcao: faz o botao iniciar a simulacao
  * @param void
  * @return void 
  * ********************************************************* */
  @FXML
  private void enviar() {
    String mensagem = getMensagemOriginal(); // Pega a mensagem do seu FXML

    // if para Validacao para nao aceitar mensagens vazias
    if (mensagem == null || mensagem.isEmpty()) {
      System.out.println("DIGITE UM TEXTO A SER TRANSMITIDO");
      return; // fim do metodo
    } // fim do if

    limparInterface(); // limpa a interface da simulacao passada

    // Iniciar a simulacao em uma thread exclusiva (logica do novo projeto)
    new Thread(() -> {
      try {
        // Chama o metodo 'startSim' da sua nova classe 'Rede'
        this.controleRede.startSim(mensagem); 
      } catch (Erro e) {
        // Mostra o erro na tela
        Platform.runLater(() -> {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Erro na Simulacao");
            alert.setHeaderText(e.getHeader());
            alert.setContentText(e.getContent());
            alert.showAndWait();
          });
        e.printStackTrace();
      } // fim do try-catch
    }).start();
  }// fim metodo

  /**************************************************************
  * Metodo: desenharSinalTransmissao
  * Funcao: (CALLBACK) enfileira o quadro para animacao
  * @param fluxoBitsTransmitido | bits a serem transmitidos
  * @return void
  * ********************************************************* */
  public void desenharSinalTransmissao(int[] fluxoBitsTransmitido) {
    // cria o no com os dados do quadro
    QuadroAnimacao novoQuadro = new QuadroAnimacao(fluxoBitsTransmitido);

    // Adiciona na fila
    if (inicioFilaAnimacao == null) {
      inicioFilaAnimacao = novoQuadro;
      fimFilaAnimacao = novoQuadro;
    } else {
      fimFilaAnimacao.proxQuadroAnimacao = novoQuadro;
      fimFilaAnimacao = novoQuadro;
    }

    // Tenta processar a fila de animacao
    processarFilaAnimacao();
  }// fim metodo desenharSinalTransmissao

  /**************************************************************
  * Metodo: processarFilaAnimacao
  * Funcao: processa a fila de animacao, se nao houver nenhuma
  * animacao em andamento
  * @param void
  * @return void
  * ********************************************************* */
  public void processarFilaAnimacao() {
    // Se uma animacao ja esta rodando, ou se a fila esta vazia, nao faz nada.
    if (animacaoEmAndamento || inicioFilaAnimacao == null) {
      return;
    }

    animacaoEmAndamento = true; // iniciamos uma animacao

    QuadroAnimacao quadroAtual = inicioFilaAnimacao; // pega o primeiro quadro para animar
    inicioFilaAnimacao = inicioFilaAnimacao.proxQuadroAnimacao; // atualiza o inicio da fila

    if (inicioFilaAnimacao == null) { // se a fila ficou vazia, atualiza o fim tambem
      fimFilaAnimacao = null;
    }

    // extrai o fluxo a ser animado
    int[] fluxoBitsTransmitido = quadroAtual.fluxoBits;

    // --- Inicio da Logica de Animacao (do novo projeto) ---
    
    // define a largura do bit simulado a partir da opcao selecionada
    final double LARGURA_BIT;
    if (codCodification() == 0) { // Binario
      LARGURA_BIT = 40.0;
    } else { // Manchester
      LARGURA_BIT = 20.0;
    }

    // pega os parametros do canvas
    final double ALTURA_GRAFICO = canvasAnimacao.getHeight();
    final double NIVEL_ALTO_Y = ALTURA_GRAFICO * 0.25;
    final double NIVEL_BAIXO_Y = ALTURA_GRAFICO * 0.75;
    final double VELOCIDADE_PX_POR_SEGUNDO = 80.0; // Velocidade do seu projeto original

    // calcular a largura total da onda em pixels
    final double LARGURA_TOTAL_DA_ONDA = fluxoBitsTransmitido.length * LARGURA_BIT;

    final long tempoInicialNano = System.nanoTime(); // define o tempo inicial da animacao

    animacao = new AnimationTimer() { // cria a animacao
      @Override
      public void handle(long now) {
        double tempoDecorridoSeg = (now - tempoInicialNano) / 1_000_000_000.0;
        double offsetX = tempoDecorridoSeg * VELOCIDADE_PX_POR_SEGUNDO;
        double posicaoInicialDaOnda = offsetX - LARGURA_TOTAL_DA_ONDA;

        // LIMPAR A TELA
        gc.clearRect(0, 0, canvasAnimacao.getWidth(), ALTURA_GRAFICO);

        // CONFIGURAR O PINCEL (com sua cor original)
        gc.setStroke(Color.web("#00FF00")); 
        gc.setLineWidth(2.5);
        gc.setLineDashes(null); // Garantir que nao esteja tracejado

        // DESENHAR A ONDA
        double nivelYAnterior = NIVEL_BAIXO_Y;

        for (int i = 0; i < fluxoBitsTransmitido.length; i++) {
          double startX = posicaoInicialDaOnda + (i * LARGURA_BIT);
          double endX = startX + LARGURA_BIT;

          if (endX < 0 || startX > canvasAnimacao.getWidth()) {
            nivelYAnterior = (fluxoBitsTransmitido[i] == 1) ? NIVEL_ALTO_Y : NIVEL_BAIXO_Y;
            continue;
          }

          // Logica de desenho de marcadores (do novo projeto)
          boolean desenharMarcador = false;
          if (codCodification() == 0) { // se for Binario
            if (i > 0) {
              desenharMarcador = true;
            }
          } else { // se nao for binario (Manchester)
            if (i > 0 && i % 2 == 0) { // somente nos pares
              desenharMarcador = true;
            }
          }

          if (desenharMarcador) {
            gc.setStroke(Color.GRAY);
            gc.setLineWidth(1); // Mais fino
            gc.setLineDashes(4);
            gc.strokeLine(startX, NIVEL_ALTO_Y - 10, startX, NIVEL_BAIXO_Y + 10);
            
            // restaura o pincel
            gc.setStroke(Color.web("#00FF00"));
            gc.setLineWidth(2.5);
            gc.setLineDashes(null);
          }
          // Fim dos marcadores
          
          double nivelYAtual = (fluxoBitsTransmitido[i] == 1) ? NIVEL_ALTO_Y : NIVEL_BAIXO_Y;

          // Desenha a transicao vertical (do seu projeto original)
          if (nivelYAtual != nivelYAnterior) {
            gc.strokeLine(startX, nivelYAnterior, startX, nivelYAtual);
          }

          gc.strokeLine(startX, nivelYAtual, endX, nivelYAtual);
          nivelYAnterior = nivelYAtual;
        }

        // condicao de parada
        if (posicaoInicialDaOnda > canvasAnimacao.getWidth()) {
          this.stop(); // para esta animacao
          animacaoEmAndamento = false; // sinaliza que a animacao terminou
          gc.clearRect(0, 0, canvasAnimacao.getWidth(), ALTURA_GRAFICO);
          processarFilaAnimacao(); // tenta processar a proxima
        }
      }
    };

    animacao.start(); // inicia a animacao configurada
  }// fim do metodo

  /**************************************************************
  * Metodo: opcaoSelecionada
  * Funcao: (HELPER) converte a string da ComboBox para um int
  * @return int | inteiro equivalente a opcao
  * ********************************************************* */
  public int codCodification() {
    String opcaoChoiceBox = comboBoxCodificacao.getValue();
    switch (opcaoChoiceBox) {
      case "Binario": return 0;
      case "Manchester": return 1;
      case "Manchester Diferencial": return 2;
      default:
        System.out.println("Problemas no metodo opcaoSelecionada");
        return 0;
    }
  }// fim do metodo

  /**************************************************************
  * Metodo: opcaoEnquadramentoSelecionada
  * Funcao: converte a string da ComboBox para um int
  * @param void
  * @return int | inteiro equivalente a opcao
  * ********************************************************* */
  public int enquadCodification() {
    String opcaoChoiceBox = comboBoxEnquadramento.getValue();
    switch (opcaoChoiceBox) {
      case "Contagem de Caracteres": return 0;
      case "Insercao de Bytes": return 1;
      case "Insercao de Bits": return 2;
      case "Violacao da Camada Fisica": return 3;
      default:
        System.out.println("Problemas no metodo opcaoEnquadramentoSelecionada");
        return 0;
    }
  }// fim do metodo

  /**************************************************************
  * Metodo: opcaoControleErroSelecionada
  * Funcao: converte a string da ComboBox para um int
  * @param void
  * @return int | inteiro equivalente a opcao
  * ********************************************************* */
  public int erroCodification() {
    String opcaoChoiceBox = comboBoxControleErro.getValue();
    switch (opcaoChoiceBox) {
      case "Bit de Paridade par": return 0;
      case "Bit de Paridade impar": return 1;
      case "CRC": return 2;
      case "Codigo de Hamming": return 3;
      default:
        System.out.println("Problemas no metodo opcaoControleErroSelecionada");
        return 0;
    }
  }// fim do metodo

  /**************************************************************
  * Metodo: opcaoControleFluxoSelecionada
  * Funcao: converte a string da ComboBox para um int
  * @param void
  * @return int | inteiro equivalente a opcao
  * ********************************************************* */
  public int fluxoCodification() {
    String opcaoChoiceBox = comboBoxFluxo.getValue();
    switch (opcaoChoiceBox) {
      case "Janela deslizante 1 bit": return 0;
      case "Go Back N": return 1;
      case "Retransmissao Seletiva": return 2;
      default:
        System.out.println("Problemas no metodo opcaoControleFluxoSelecionada");
        return 0;
    }
  }// fim do metodo

  /**************************************************************
  * Metodo: getMensagemOriginal
  * Funcao: Pega o texto original da GUI
  * @param void
  * @return String | texto digitado
  * ********************************************************* */
  public String getMensagemOriginal() {
    return textAreaMensagemOriginal.getText();
  }

  /**************************************************************
  * Metodo: exibirMensagemRecebida
  * Funcao: mostra a mensagem final na GUI
  * @param mensagemRecebida | a string que sera exibida
  * @return void
  * ********************************************************* */
  public void exibirMensagemRecebida(String mensagemRecebida) {
    Platform.runLater(() -> {
      // Usa o 'appendText' para construir a mensagem final
      textAreaMensagemFinal.appendText(mensagemRecebida);
    });
  }// fim mensagem recebida

  /**************************************************************
  * Metodo: exibirMensagemBinariaTransmitida
  * Funcao: exibe os bits da aplicacao TX
  * @param binarioMensagem | array de int com a informacao em binario
  * @return void
  * ********************************************************* */
  public void exibirMensagemBinariaTransmitida(int[] binarioMensagem) {
    Platform.runLater(() -> {
      textAreaCodificada.setText(Auxiliares.showBits(binarioMensagem));
    });
  }// fim metodo

  /**************************************************************
  * Metodo: exibirMensagemBinariaRecebida
  * Funcao: exibe os bits da aplicacao RX
  * @param binarioMensagem | array de int com a informacao em binario
  * @return void
  * ********************************************************* */
  public void exibirMensagemBinariaRecebida(int[] binarioMensagem) {
    Platform.runLater(() -> {
      StringBuilder sb = new StringBuilder();
      sb.append(Auxiliares.showBits(binarioMensagem));
      sb.append("\n");
      textAreaDecodificada.appendText(sb.toString());
    }); 
  }// fim metodo

  /**************************************************************
  * Metodo: getValorTaxaErro
  * Funcao: retorna o valor em double da taxa de erro
  * @param void
  * @return double | valor double da taxa de erro
  * ********************************************************* */
  public double getValorTaxaErro() {
    String valorSelecionado = comboBoxErro.getValue();

    if (valorSelecionado == null || valorSelecionado.isEmpty()) {
      return 0.0; // retorna 0 se nada for selecionado
    }

    try {
      // remove o caractere '%' e converte o resto da string para um numero
      String numeroString = valorSelecionado.replace("%", "").trim();
      int valorInteiro = Integer.parseInt(numeroString);
      return valorInteiro / 100.0; // Converte a porcentagem para um valor double
    } catch (NumberFormatException e) {
      e.printStackTrace();
      return 0.0; // retorna 0 em caso de erro na conversao
    } // fim try
  } // fim getValorTaxaErro

  /**************************************************************
  * Metodo: limparInterface
  * Funcao: limpa a interface a cada nova transmissao
  * @param void
  * @return void
  * ********************************************************* */
  public void limparInterface() {
    textAreaMensagemFinal.clear();
    textAreaDecodificada.clear();
    textAreaCodificada.clear();

    // Para qualquer animacao que esteja rodando
    if (animacao != null) {
      animacao.stop();
    }
    // Limpa a fila de quadros pendentes
    inicioFilaAnimacao = null;
    fimFilaAnimacao = null;
    animacaoEmAndamento = false;

    // Limpa o canvas imediatamente
    if (gc != null) {
      gc.clearRect(0, 0, canvasAnimacao.getWidth(), canvasAnimacao.getHeight());
    }
  } // fim limparInterface

} // fim da classe