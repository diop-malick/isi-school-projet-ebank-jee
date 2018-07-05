package com.examen.GestionBanque.controller;

import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.examen.GestionBanque.dao.AgenceRepository;
import com.examen.GestionBanque.dao.CompteRepository;
import com.examen.GestionBanque.dao.EmployeRepository;
import com.examen.GestionBanque.dao.OperationRepository;
import com.examen.GestionBanque.dao.UserRepository;
import com.examen.GestionBanque.entities.Compte;
import com.examen.GestionBanque.entities.CompteBloque;
import com.examen.GestionBanque.entities.CompteCourant;
import com.examen.GestionBanque.entities.CompteEpargne;
import com.examen.GestionBanque.entities.Operation;
import com.examen.GestionBanque.enums.OperationStatus;
import com.examen.GestionBanque.enums.OperationType;
import com.examen.GestionBanque.enums.TransactionType;
import com.examen.GestionBanque.service.CompteService;
import com.examen.GestionBanque.service.UserService;

@Controller
@RequestMapping("/compte")
public class CompteController {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private CompteService compteService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private UserService userService;

	@Autowired
	private AgenceRepository agenceRepository;

	@Autowired
	private CompteRepository compteRepository;
	
	@Autowired
	private EmployeRepository employeRepository;

	@Autowired
	private OperationRepository operationRepository;

	/**
	 * Affiche la liste des comptes
	 */
	@RequestMapping(value = "/liste")
	public String liste(Model model) {
		List<Compte> comptes = compteRepository.findAll();
		model.addAttribute("comptes", comptes);
		return "compte/liste";
	}

	/*
	 * Afficher détail d'un compte à partir de son numéro
	 */
	@RequestMapping(value = "/detail/{num}")
	public String detail(@PathVariable String num, Model model, Pageable pageable) {
		Optional<Compte> compte = compteRepository.findById(num);

		if (compte.isPresent()) {
			log.info(compte.toString());
			model.addAttribute("compte", compte.get());

			Page<Operation> operations = operationRepository
					.findByCompteNumCompteOrderByDateAsc(compte.get().getNumCompte(), pageable);
			model.addAttribute("operations", operations);

			// Initialiser formulaire Enregsitrement Opération
			model.addAttribute("operation", new Operation(compte.get()));

			model.addAttribute("typeOperations",
					Arrays.asList(OperationType.DEPOT, OperationType.RETRAIT, OperationType.VIREMENT));
		}

		return "compte/detail";
	}

	/*
	 * Affiche le formulaire d'ouverture de compte
	 */
	@GetMapping("/ouverture")
	public String registration(Model model, String typeComte) {

		if (typeComte.equals("CE")) {
			CompteEpargne compte = new CompteEpargne();
			model.addAttribute("compte", compte);
		} else if (typeComte.equals("CC")) {
			model.addAttribute("compte", new CompteCourant());
		} else {
			CompteBloque compte = new CompteBloque();
			model.addAttribute("compte", compte);
		}

		model.addAttribute("users", userService.findAll());
		model.addAttribute("agences", agenceRepository.findAll());
		return "compte/ouverture";
	}

	/*
	 * Enregistre les données renvoyées par le formulaire d'ouverture de compte
	 */
	@PostMapping("/ouverture")
	public String ajoutNouveauCompte(@Valid CompteCourant compte, Long idClient, String codeAgence, Long idEmploye,
			BindingResult bindingResult, RedirectAttributes attributes, Model model) {

		log.debug("Controller Service save Compte");
		log.debug("codeAgence =" + codeAgence + "/ idClient =" + idClient + "idEmploye="  + idEmploye);
		log.debug(compte.toString());

		if (bindingResult.hasErrors()) {
			return "compte/ouverture";
		} else {
			compte.setAgence(agenceRepository.getOne(codeAgence));
			/*
			 * Ajout d'un responsable de compte dans le formulaire d'ouverture de compte
			 */
			compte.setEmploye(employeRepository.getOne(idEmploye));
			compte.setClient(userRepository.getOne(idClient).getClient());
			compte.setDateCreation(new Date());
			Compte compteEnregistre = compteService.saveCompte(compte);

			model.addAttribute("successMessage", "le compte a été créer avec succés");
			model.addAttribute("compte", compteEnregistre);

			// Initialiser formulaire Enregsitrement Opération
			model.addAttribute("operation", new Operation(compteEnregistre));
		}
		return "compte/detail";
	}

	/*
	 * Enregistre les données envoyéess pour une nouvelle opération
	 */
	@PostMapping("/operation")
	@Transactional
	public String ajoutNouvelleOperation(@Valid Operation operation, boolean sms, String numCompte,
			BindingResult bindingResult, RedirectAttributes attributes, Model model) {

		log.info("Controller - Service Ajout nouvelle Opération");
		log.info("OPERATION : " + operation.toString());
		log.info("sms : " + sms);
		log.info("numCompte : " + numCompte);

		if (bindingResult.hasErrors()) {

			log.info(bindingResult.toString());
			return "compte/detail";

		} else {

			// Récupération du compte
			Compte compte = compteRepository.findById(numCompte).get();
			operation.setCompte(compte);

			// date d'ajout
			operation.setDate(Instant.now());
			operation.setStatusOperation(OperationStatus.EXECUTEE);

			// Type transaction
			if (operation.getTypeOperation().equals(OperationType.DEPOT)) {
				operation.setTypeTransaction(TransactionType.CREDIT);
			} else {
				// Retrait, virement
				operation.setTypeTransaction(TransactionType.DEBIT);
			}

			// Taxes opération automatique
			operation.setTaxeOperation(getTaxeOperation());

			// Taxes sms Si l'option sms est choisi
			if (sms == true) {
				operation.setTaxeSms(getMontantTaxeSms());
			}

			// Total TTC selon que c'est un crédit ou un débit
			if ((operation.getTypeTransaction().equals(TransactionType.CREDIT))) {
				operation.setMontantTTC(
						operation.getMontantHT() - operation.getTaxeSms() - operation.getTaxeOperation());
			} else {
				operation.setMontantTTC(
						operation.getMontantHT() + operation.getTaxeSms() + operation.getTaxeOperation());
			}

			// Mise à jour solde compte
			if ((operation.getTypeTransaction().equals(TransactionType.CREDIT))) {
				compte.setSolde(compte.getSolde() + operation.getMontantTTC());
			} else {
				if (compte.getSolde() >= operation.getMontantTTC()) {
					compte.setSolde(compte.getSolde() - operation.getMontantTTC());
				} else {
					attributes.addFlashAttribute("errorMessage",
							"L'opération n'a pas été exécuté !<br /> Solde Insuffisant");
				}
			}

			if ((operation.getTypeTransaction().equals(TransactionType.CREDIT)
					|| compte.getSolde() >= operation.getMontantTTC())) {
				// Enregsitrement de l'opération
				Operation operationEnregsitree = operationRepository.save(operation);
				// Enregsitrement compte mis à jour
				Compte compteEnregistre = compteService.saveCompte(compte);
				model.addAttribute("compte", compteEnregistre);

				if (operationEnregsitree != null && compteEnregistre != null) {
					attributes.addFlashAttribute("successMessage",
							"L'opération " + operation.getTypeOperation() + " numéro " + operationEnregsitree.getId()
									+ " a été exécutée <br /> le solde du compte mis à jour avec succés");
				}
			}

		}
		return "redirect:" + "/compte/detail/" + operation.getCompte().getNumCompte();

	}

	/*
	 * Retourne le montant de la taxe si l'option sms est choisie
	 */
	private double getMontantTaxeSms() {
		return 5;
	}

	/*
	 * Retourne le montant taxé pour chaque opération
	 */
	private double getTaxeOperation() {
		return 10;
	}

}
