
import java.io.IOException;

public class Gatito {

    public static void registrarJugada(char caracter) throws IOException{
    boolean salir = false;
    String entrada;
    
    int posicion = 0;

    if(casillaNoOcupada(posicion)){
        switch(posicion){
        case 1: gato[0][0] = caracter;
                break;
        case 2: gato[0][1] = caracter;
                break;  
        case 3: gato[0][2] = caracter;
                break;
        case 4: gato[1][0] = caracter;
                break;
        case 5: gato[1][1] = caracter;                  
                break;
            




        }



    }


    }

    
	


	

}
