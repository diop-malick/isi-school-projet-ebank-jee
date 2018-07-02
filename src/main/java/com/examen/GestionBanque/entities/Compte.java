package com.examen.GestionBanque.entities;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.validation.constraints.NotNull;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type_cpte", discriminatorType = DiscriminatorType.STRING, length = 30)
public abstract class Compte implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	@NotNull
	@Column(name = "numero_compte", length = 20)
	private String numCompte;

	@NotNull
	@Column(name = "solde")
	private double solde;

	@Column(name = "date_ouverture")
	private Date dateCreation;

	@Column(nullable = false)
	private boolean etat = true;

	@ManyToOne
	@JoinColumn(name = "code_employe")
	private Employe employe;

	@ManyToOne
	@JoinColumn(name = "code_agence")
	private Agence agence;

	@ManyToOne
	@JoinColumn(name = "code_client")
	private Client client;

	@OneToMany(mappedBy = "compte", fetch = FetchType.LAZY)
	private List<Operation> operations;

	@OneToMany(mappedBy = "compte", fetch = FetchType.LAZY)
	private List<HistoriqueTaxe> historiques;

	/* Contructeurs */

	public Compte() {
	}

	public Compte(@NotNull String numCompte, @NotNull double solde, @NotNull Date dateCreation, boolean etat) {
		super();
		this.numCompte = numCompte;
		this.solde = solde;
		this.dateCreation = dateCreation;
		this.etat = etat;
	}

	/* Getters & Setters */

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getNumCompte() {
		return numCompte;
	}

	public void setNumCompte(String numCompte) {
		this.numCompte = numCompte;
	}

	public double getSolde() {
		return solde;
	}

	public void setSolde(double solde) {
		this.solde = solde;
	}

	public Date getDateCreation() {
		return dateCreation;
	}

	public void setDateCreation(Date dateCreation) {
		this.dateCreation = dateCreation;
	}

	public boolean isEtat() {
		return etat;
	}

	public void setEtat(boolean etat) {
		this.etat = etat;
	}

	public Employe getEmploye() {
		return employe;
	}

	public void setEmploye(Employe employe) {
		this.employe = employe;
	}

	public Agence getAgence() {
		return agence;
	}

	public void setAgence(Agence agence) {
		this.agence = agence;
	}

	public Client getClient() {
		return client;
	}

	public void setClient(Client client) {
		this.client = client;
	}

	public List<Operation> getOperations() {
		return operations;
	}

	public void setOperations(List<Operation> operations) {
		this.operations = operations;
	}

	public List<HistoriqueTaxe> getHistoriques() {
		return historiques;
	}

	public void setHistoriques(List<HistoriqueTaxe> historiques) {
		this.historiques = historiques;
	}

	@Override
	public String toString() {
		return "Compte [id=" + id + ", numCompte=" + numCompte + ", solde=" + solde + ", dateCreation=" + dateCreation
				+ ", etat=" + etat + ", employe=" + employe + ", agence=" + agence + ", client=" + client
				+ ", operations=" + operations + ", historiques=" + historiques + "]";
	}

}
