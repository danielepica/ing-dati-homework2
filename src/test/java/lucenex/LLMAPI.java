package lucenex;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

/*Attualmente, Hugging Face fornisce una libreria chiamata Transformers che offre modelli di linguaggio
pre-addestrati per svariate attività, inclusa la generazione di riassunti.
Puoi utilizzare questa libreria per integrare un modello di linguaggio pre-addestrato nel tuo codice Java per
generare riassunti. La libreria Transformers di Hugging Face fornisce modelli pre-addestrati che puoi scaricare
direttamente dall'Hugging Face Model Hub. Per un modello di generazione di riassunti, puoi cercare un modello
come facebook/bart-large-cnn. Ecco come puoi fare:

Trova il Modello su Hugging Face Model Hub:
Vai su Hugging Face Model Hub e cerca il modello che desideri utilizzare per la generazione di riassunti. Ad esempio, cerca facebook/bart-large-cnn.

Scarica il Modello:
Una volta trovato il modello, puoi scaricarlo facendo clic sul pulsante "Use in Transformers" e copiando il percorso. Ad esempio, il percorso potrebbe essere qualcosa del genere: facebook/bart-large-cnn.

Utilizza il Modello nel Codice:
Sostituisci il percorso del modello nel tuo codice Java. Ad esempio, modifica la linea:*/

public class LLMAPI {

    public static void main(String[] args) {
        System.out.println(chatLLM("ChatGPT è un modello di linguaggio machine avanzato sviluppato da OpenAI basato learning sull'architettura GPT-3.5. Questo sistema di conversazione virtuale può comprendere e generare testo in modo coerente e contestualmente rilevante. Con un vasto training su diverse fonti di informazione, ChatGPT è versatile, capace di rispondere a domande, generare testo creativo e sostenere conversazioni in modo naturale. Tuttavia, è importante notare che ChatGPT può occasionalmente produrre risposte inaccurate o non coerenti e che deve essere utilizzato con discernimento. Il suo utilizzo spazia da supporto informativo e creativo a esercizi di scrittura guidata.\n"));
        // Prints out a response to the question.
    }

    public static String chatLLM(String message) {
        String messageError = "Ops, qualcosa non è andata nel verso giusto. Riprova!";
        String url = "https://api-inference.huggingface.co/models/it5/it5-base-news-summarization";
        String apiKey = "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"; // API key goes here
        StringBuilder response = new StringBuilder();

        try {
            // Create the HTTP POST request
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Authorization", "Bearer " + apiKey);
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("Accept", "application/json");

            String inputJson = "{\"inputs\": \"" + message + "\" }";

            con.setDoOutput(true);

            try (OutputStream os = con.getOutputStream()) {
                byte[] inputBytes = inputJson.getBytes("utf-8");
                os.write(inputBytes, 0, inputBytes.length);
            }

            // Get the response
            try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), "utf-8"))) {
                String responseLine = null;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
            }

            con.disconnect();

        } catch (IOException e) {
            return messageError;
        }
        return response.toString();
    }

}


