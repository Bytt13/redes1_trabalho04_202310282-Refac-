/* **************************************************************** 
* Autor............: Lucas de Menezes Chaves
* Matricula........: 202310282
* Inicio...........: 18/08/2025
* Ultima alteracao.: 31/10/2025
* Nome.............: Principal
* Funcao...........: Roda o programa com o comando javac Principal.java
*************************************************************** */
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import controller.TelaPrincipalController;
import util.Auxiliares;
import util.Erro;
import controller.Host;
import controller.Rede;
import model.AplicacaoReceptora;
import model.AplicacaoTransmissora;
import model.CamadaAplicacaoReceptora;
import model.CamadaAplicacaoTransmissora;
import model.CamadaFisicaReceptora;
import model.CamadaFisicaTransmissora;
import model.MeioDeComunicacao;
import model.CamadaEnlaceDadosTransmissora;
import model.CamadaEnlaceDadosReceptora;


@SuppressWarnings("unused")
public class Principal extends Application{
  /****************************************************************
  * Metodo: start
  * Funcao: carrega os elementos fxml para tela
  * @param stage O palco principal da aplicacao
  * @throws Exception para casos de erros
  * @return void 
  * *********************************************************** */
  @Override
  public void start(Stage stage) throws Exception {
    FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/TelaPrincipalRedes.fxml")); //Cria o loader que carrega os dados do arquivo fxml
    Parent root = loader.load(); //Monta de fato o cenario
    Scene scene = new Scene(root); //Cria uma cena para o cenario, que armazena os dados dele
    stage.setTitle("Simulador de Camada Fisica"); //Adiciona um titulo a janela
    stage.setScene(scene); //Coloca a cena criada para rodar dentro da janela
    stage.setResizable(false); //Nao deixa a janela ser redimensionavel
    stage.setOnCloseRequest(event -> {
    Platform.exit();
    System.exit(0); //Garante que o sistema feche por completo ao fechar a janela
  });
  stage.show(); //Faz a janela ser visivel
  } // Fim do metodo

  /****************************************************************
  * Metodo: main
  * Funcao: roda o programa java
  * @param args Argumentos da linha de comando
  * @return void 
  * ************************************************************ */
  public static void main(String[] args) {
    launch(args); // Inicializa o programa
  } // fim do metodo
} // fim da classe