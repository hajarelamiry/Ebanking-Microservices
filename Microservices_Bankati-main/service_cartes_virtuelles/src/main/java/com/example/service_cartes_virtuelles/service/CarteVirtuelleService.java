package com.example.service_cartes_virtuelles.service;

import com.example.service_cartes_virtuelles.mapper.CarteVirtuelleMapper;
import com.example.service_cartes_virtuelles.mapper.TransactionMapper;
import com.example.service_cartes_virtuelles.repo.CarteVirtuelleRepository;
import com.example.service_cartes_virtuelles.repo.TransactionRepository;
import org.example.dto.CarteVirtuelleDTO;
import org.example.dto.TransactionDTO;
import org.example.entites.CarteVirtuelle;
import org.example.entites.Transaction;
import org.example.enums.Devise;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CarteVirtuelleService {

    @Autowired
    private CarteVirtuelleRepository carteVirtuelleRepository;

    @Autowired
    private CarteVirtuelleMapper carteVirtuelleMapper;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private ExchangeRateService exchangeRateService;

    @Autowired
    private TransactionMapper transactionMapper;

    private static final String ALGORITHM = "AES";
    private static final String SECRET_KEY = "1234567890abcdef1234567890abcdef";

    public String creerCarte(CarteVirtuelleDTO carteVirtuelleDTO) throws Exception {
        // Vérifier si l'utilisateur possède déjà une carte pour la devise donnée
        Long utilisateurId = carteVirtuelleDTO.getUtilisateurId();
        Devise devise = carteVirtuelleDTO.getDevise();

        // Récupérer toutes les cartes de l'utilisateur pour vérifier les devises existantes
        List<CarteVirtuelle> cartesExistantes = carteVirtuelleRepository.findByUtilisateurId(utilisateurId);

        // Vérifier s'il existe déjà une carte pour cette devise
        boolean deviseExiste = cartesExistantes.stream()
                .anyMatch(carte -> carte.getDevise().equals(devise));

        if (deviseExiste) {
            return "L'utilisateur possède déjà une carte avec la devise : " + devise;
        }

        // Vérifier si l'utilisateur a déjà 3 cartes
        if (cartesExistantes.size() >= 3) {
            return "L'utilisateur possède déjà trois cartes virtuelles.";
        }

        // Mapper le DTO en entité et générer les détails spécifiques de la carte virtuelle
        CarteVirtuelle carteVirtuelle = carteVirtuelleMapper.toEntity(carteVirtuelleDTO);
        carteVirtuelle.setNumero_carte(genererNumeroCarte());
        carteVirtuelle.setCvv(genererCVV());
        carteVirtuelle.setDate_expiration(LocalDate.now().plusYears(1));
        carteVirtuelle.setStatus("Active");

        // Sauvegarder la carte dans le dépôt
        CarteVirtuelle savedCarte = carteVirtuelleRepository.save(carteVirtuelle);

        // Retourner un message de succès avec les détails de la carte
        return "Carte virtuelle créée avec succès. Numéro de carte: " + savedCarte.getNumero_carte();
    }


    // Méthode pour générer le numéro de carte virtuel (exemple simple)
    private String genererNumeroCarte() {
        return "411111111111" + (int) (Math.random() * 10000); // Exemple simple
    }

    // Méthode pour générer un CVV à 3 chiffres
    private String genererCVV() {
        return String.valueOf((int) (Math.random() * 900) + 100); // CVV aléatoire
    }

    // Méthode pour crypter le numéro de carte
    public String crypterNumeroCarte(String numeroCarte) throws Exception {
        String cle = "1234567890123456"; // Exemple de clé AES valide (16 octets)
        SecretKeySpec cleSpec = new SecretKeySpec(cle.getBytes(StandardCharsets.UTF_8), "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, cleSpec);
        byte[] encrypted = cipher.doFinal(numeroCarte.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encrypted);
    }


    // Méthode pour crypter le CVV
    private String crypterCVV(String cvv) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(SECRET_KEY.getBytes(), ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encrypted = cipher.doFinal(cvv.getBytes());
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors du cryptage du CVV", e);
        }
    }

    // Méthode pour décrypter les données lorsque nécessaire
    public String decrypter(String encryptedData) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(SECRET_KEY.getBytes(), ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decoded = Base64.getDecoder().decode(encryptedData);
            byte[] decrypted = cipher.doFinal(decoded);
            return new String(decrypted);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors du décryptage des données", e);
        }
    }

    // Méthode pour supprimer une carte virtuelle
    public boolean supprimerCarte(Long id) {
        if (carteVirtuelleRepository.existsById(id)) {
            carteVirtuelleRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public List<CarteVirtuelleDTO> getCartesParUtilisateur(Long utilisateurId) {
        List<CarteVirtuelle> cartes = carteVirtuelleRepository.findByUtilisateurId(utilisateurId);
        return cartes.stream()
                .map(carteVirtuelleMapper::toDTO)
                .collect(Collectors.toList());
    }

    public CarteVirtuelleDTO getCarteByCvv(String cvv) {
        CarteVirtuelle carteVirtuelle = carteVirtuelleRepository.findByCvv(cvv)
                .orElseThrow(() -> new RuntimeException("Carte virtuelle non trouvée avec le CVV : " + cvv));

        // Utiliser le mapper pour convertir en DTO
        return carteVirtuelleMapper.toDTO(carteVirtuelle);
    }

    public void debitCard(Long id, Double amount) {
        CarteVirtuelle carte = carteVirtuelleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Carte virtuelle introuvable"));

        if (carte.getLimite() < amount) {
            throw new RuntimeException("Fonds insuffisants");
        }

        carte.setLimite(carte.getLimite() - amount);
        carteVirtuelleRepository.save(carte);
    }

    public Long getCardIdByCvv(String cvv) {
        return carteVirtuelleRepository.findIdByCvv(cvv);
    }






    public String processPaymentWithVirtualCard(String cvv, String toCurrency, Double amount) {
        // 1. Vérification des détails de la carte virtuelle
        CarteVirtuelleDTO carteVirtuelle = getCarteByCvv(cvv);

        if (carteVirtuelle.getLimite() < amount) {
            throw new RuntimeException("Fonds insuffisants sur la carte virtuelle");
        }

        // 2. Conversion de devise si nécessaire
        Double convertedAmount = amount;
        if (!carteVirtuelle.getDevise().equals(toCurrency)) {
            Double exchangeRate = exchangeRateService.getExchangeRate(carteVirtuelle.getDevise(), Devise.valueOf(toCurrency));
            convertedAmount = amount * exchangeRate;
        }

        Long cardId = getCardIdByCvv(cvv);
        // 3. Débiter le montant de la carte virtuelle
        debitCard(cardId, amount);

        // 4. Enregistrer la transaction
        Transaction transaction = new Transaction();
        transaction.setCarteVirtuelle(carteVirtuelleMapper.toEntity(carteVirtuelle)); // Conversion DTO en entité
        transaction.setDestinateur(null); // Pas de portefeuille spécifique pour une carte virtuelle
        transaction.setDestinataire(null); // Ajoutez un destinataire si applicable
        transaction.setMontant(amount);
        transaction.setStatus("COMPLETED");
        transaction.setDate(LocalDateTime.now());

        transactionRepository.save(transaction);

        // 5. Retourner la confirmation
        System.out.println("Transaction réussie : Montant converti = " + convertedAmount + " " + toCurrency);
        return "Paiement réussi de " + convertedAmount + " " + toCurrency;
    }



    public List<TransactionDTO> getTransactionsByCarteVirtuelleId(String cvv) {
        // Fetch the virtual card ID using the CVV
        Long carteVirtuelleId = carteVirtuelleRepository.findIdByCvv(cvv);
        System.out.println("id :" + carteVirtuelleId);

        // Handle the case where no card is found
        if (carteVirtuelleId == null) {
            throw new RuntimeException("No virtual card found for the provided CVV: " + cvv);
        }

        // Ensure that findByCarteVirtuelleId returns a list of Transaction objects
        List<Transaction> transactions = transactionRepository.findByCarteVirtuelleId(carteVirtuelleId);

        // Fetch transactions and map them to DTOs using stream
        return transactions.stream()
                .map(TransactionMapper::toDTO)  // Assuming toDTO is a method to map Transaction to TransactionDTO
                .collect(Collectors.toList());
    }



}
