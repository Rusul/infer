/* CVS $Id: $ */
package infer.util.generated; 
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.ontology.*;
 
/**
 * Vocabulary definitions from vcard.owl 
 * @author Auto-generated by schemagen on 02 Sep 2014 11:49 
 */
public class Vcard {
    /** <p>The ontology model that holds the vocabulary terms</p> */
    private static OntModel m_model = ModelFactory.createOntologyModel( OntModelSpec.OWL_MEM, null );
    
    /** <p>The namespace of the vocabulary as a string</p> */
    public static final String NS = "http://www.w3.org/2006/vcard/ns#";
    
    /** <p>The namespace of the vocabulary as a string</p>
     *  @see #NS */
    public static String getURI() {return NS;}
    
    /** <p>The namespace of the vocabulary as a resource</p> */
    public static final Resource NAMESPACE = m_model.createResource( NS );
    
    /** <p>A postal or street address of a person</p> */
    public static final ObjectProperty adr = m_model.createObjectProperty( "http://www.w3.org/2006/vcard/ns#adr" );
    
    /** <p>A person that acts as one's agent</p> */
    public static final ObjectProperty agent = m_model.createObjectProperty( "http://www.w3.org/2006/vcard/ns#agent" );
    
    /** <p>An email address</p> */
    public static final ObjectProperty email = m_model.createObjectProperty( "http://www.w3.org/2006/vcard/ns#email" );
    
    /** <p>A geographic location associated with a person</p> */
    public static final ObjectProperty geo = m_model.createObjectProperty( "http://www.w3.org/2006/vcard/ns#geo" );
    
    /** <p>A key (e.g, PKI key) of a person</p> */
    public static final ObjectProperty key = m_model.createObjectProperty( "http://www.w3.org/2006/vcard/ns#key" );
    
    /** <p>A logo associated with a person or their organization</p> */
    public static final ObjectProperty logo = m_model.createObjectProperty( "http://www.w3.org/2006/vcard/ns#logo" );
    
    /** <p>The components of the name of a person</p> */
    public static final ObjectProperty n = m_model.createObjectProperty( "http://www.w3.org/2006/vcard/ns#n" );
    
    /** <p>An organization associated with a person</p> */
    public static final ObjectProperty org = m_model.createObjectProperty( "http://www.w3.org/2006/vcard/ns#org" );
    
    /** <p>A photograph of a person</p> */
    public static final ObjectProperty photo = m_model.createObjectProperty( "http://www.w3.org/2006/vcard/ns#photo" );
    
    /** <p>A sound (e.g., a greeting or pronounciation) of a person</p> */
    public static final ObjectProperty sound = m_model.createObjectProperty( "http://www.w3.org/2006/vcard/ns#sound" );
    
    /** <p>A telephone number of a person</p> */
    public static final ObjectProperty tel = m_model.createObjectProperty( "http://www.w3.org/2006/vcard/ns#tel" );
    
    /** <p>A URL associated with a person</p> */
    public static final ObjectProperty url = m_model.createObjectProperty( "http://www.w3.org/2006/vcard/ns#url" );
    
    /** <p>An additional part of a person's name</p> */
    public static final DatatypeProperty additional_name = m_model.createDatatypeProperty( "http://www.w3.org/2006/vcard/ns#additional-name" );
    
    /** <p>The birthday of a person</p> */
    public static final DatatypeProperty bday = m_model.createDatatypeProperty( "http://www.w3.org/2006/vcard/ns#bday" );
    
    /** <p>A category of a vCard</p> */
    public static final DatatypeProperty category = m_model.createDatatypeProperty( "http://www.w3.org/2006/vcard/ns#category" );
    
    /** <p>A class (e.g., public, private, etc.) of a vCard</p> */
    public static final DatatypeProperty class_ = m_model.createDatatypeProperty( "http://www.w3.org/2006/vcard/ns#class" );
    
    /** <p>The country of a postal address</p> */
    public static final DatatypeProperty country_name = m_model.createDatatypeProperty( "http://www.w3.org/2006/vcard/ns#country-name" );
    
    /** <p>The extended address of a postal address</p> */
    public static final DatatypeProperty extended_address = m_model.createDatatypeProperty( "http://www.w3.org/2006/vcard/ns#extended-address" );
    
    /** <p>A family name part of a person's name</p> */
    public static final DatatypeProperty family_name = m_model.createDatatypeProperty( "http://www.w3.org/2006/vcard/ns#family-name" );
    
    /** <p>A formatted name of a person</p> */
    public static final DatatypeProperty fn = m_model.createDatatypeProperty( "http://www.w3.org/2006/vcard/ns#fn" );
    
    /** <p>A given name part of a person's name</p> */
    public static final DatatypeProperty given_name = m_model.createDatatypeProperty( "http://www.w3.org/2006/vcard/ns#given-name" );
    
    /** <p>An honorific prefix part of a person's name</p> */
    public static final DatatypeProperty honorific_prefix = m_model.createDatatypeProperty( "http://www.w3.org/2006/vcard/ns#honorific-prefix" );
    
    /** <p>An honorific suffix part of a person's name</p> */
    public static final DatatypeProperty honorific_suffix = m_model.createDatatypeProperty( "http://www.w3.org/2006/vcard/ns#honorific-suffix" );
    
    /** <p>The formatted version of a postal address (a string with embedded line breaks, 
     *  punctuation, etc.)</p>
     */
    public static final DatatypeProperty label = m_model.createDatatypeProperty( "http://www.w3.org/2006/vcard/ns#label" );
    
    /** <p>The latitude of the location of the vCard object</p> */
    public static final DatatypeProperty latitude = m_model.createDatatypeProperty( "http://www.w3.org/2006/vcard/ns#latitude" );
    
    /** <p>The locality (e.g., city) of a postal address</p> */
    public static final DatatypeProperty locality = m_model.createDatatypeProperty( "http://www.w3.org/2006/vcard/ns#locality" );
    
    /** <p>The longitude of the location of the vCard object</p> */
    public static final DatatypeProperty longitude = m_model.createDatatypeProperty( "http://www.w3.org/2006/vcard/ns#longitude" );
    
    /** <p>A mailer associated with a vCard</p> */
    public static final DatatypeProperty mailer = m_model.createDatatypeProperty( "http://www.w3.org/2006/vcard/ns#mailer" );
    
    /** <p>The nickname of a person</p> */
    public static final DatatypeProperty nickname = m_model.createDatatypeProperty( "http://www.w3.org/2006/vcard/ns#nickname" );
    
    /** <p>Notes about a person on a vCard</p> */
    public static final DatatypeProperty note = m_model.createDatatypeProperty( "http://www.w3.org/2006/vcard/ns#note" );
    
    /** <p>The name of an organization</p> */
    public static final DatatypeProperty organization_name = m_model.createDatatypeProperty( "http://www.w3.org/2006/vcard/ns#organization-name" );
    
    /** <p>The name of a unit within an organization</p> */
    public static final DatatypeProperty organization_unit = m_model.createDatatypeProperty( "http://www.w3.org/2006/vcard/ns#organization-unit" );
    
    /** <p>The post office box of a postal address</p> */
    public static final DatatypeProperty post_office_box = m_model.createDatatypeProperty( "http://www.w3.org/2006/vcard/ns#post-office-box" );
    
    /** <p>The postal code (e.g., U.S. ZIP code) of a postal address</p> */
    public static final DatatypeProperty postal_code = m_model.createDatatypeProperty( "http://www.w3.org/2006/vcard/ns#postal-code" );
    
    /** <p>The Identifier for the product that created the vCard object</p> */
    public static final DatatypeProperty prodid = m_model.createDatatypeProperty( "http://www.w3.org/2006/vcard/ns#prodid" );
    
    /** <p>The region (e.g., state or province) of a postal address</p> */
    public static final DatatypeProperty region = m_model.createDatatypeProperty( "http://www.w3.org/2006/vcard/ns#region" );
    
    /** <p>The timestamp of a revision of a vCard</p> */
    public static final DatatypeProperty rev = m_model.createDatatypeProperty( "http://www.w3.org/2006/vcard/ns#rev" );
    
    /** <p>A role a person plays within an organization</p> */
    public static final DatatypeProperty role = m_model.createDatatypeProperty( "http://www.w3.org/2006/vcard/ns#role" );
    
    /** <p>A version of a person's name suitable for collation</p> */
    public static final DatatypeProperty sort_string = m_model.createDatatypeProperty( "http://www.w3.org/2006/vcard/ns#sort-string" );
    
    /** <p>The street address of a postal address</p> */
    public static final DatatypeProperty street_address = m_model.createDatatypeProperty( "http://www.w3.org/2006/vcard/ns#street-address" );
    
    /** <p>A person's title</p> */
    public static final DatatypeProperty title = m_model.createDatatypeProperty( "http://www.w3.org/2006/vcard/ns#title" );
    
    /** <p>A timezone associated with a person</p> */
    public static final DatatypeProperty tz = m_model.createDatatypeProperty( "http://www.w3.org/2006/vcard/ns#tz" );
    
    /** <p>A UID of a person's vCard</p> */
    public static final DatatypeProperty uid = m_model.createDatatypeProperty( "http://www.w3.org/2006/vcard/ns#uid" );
    
    /** <p>Resources that are vCard Addresses</p> */
    public static final OntClass Address = m_model.createClass( "http://www.w3.org/2006/vcard/ns#Address" );
    
    /** <p>Bulletin Board System Communications</p> */
    public static final OntClass BBS = m_model.createClass( "http://www.w3.org/2006/vcard/ns#BBS" );
    
    /** <p>Car Telephone</p> */
    public static final OntClass Car = m_model.createClass( "http://www.w3.org/2006/vcard/ns#Car" );
    
    /** <p>Cellular or Mobile Telephone</p> */
    public static final OntClass Cell = m_model.createClass( "http://www.w3.org/2006/vcard/ns#Cell" );
    
    /** <p>Information related to a Domestic Address or Label</p> */
    public static final OntClass Dom = m_model.createClass( "http://www.w3.org/2006/vcard/ns#Dom" );
    
    /** <p>Resources that are vCard Email Addresses</p> */
    public static final OntClass Email = m_model.createClass( "http://www.w3.org/2006/vcard/ns#Email" );
    
    /** <p>Fax Communications</p> */
    public static final OntClass Fax = m_model.createClass( "http://www.w3.org/2006/vcard/ns#Fax" );
    
    /** <p>Information related to a Home Address, Label, or Telephone</p> */
    public static final OntClass Home = m_model.createClass( "http://www.w3.org/2006/vcard/ns#Home" );
    
    /** <p>ISDN Communications</p> */
    public static final OntClass ISDN = m_model.createClass( "http://www.w3.org/2006/vcard/ns#ISDN" );
    
    /** <p>Internet Email</p> */
    public static final OntClass Internet = m_model.createClass( "http://www.w3.org/2006/vcard/ns#Internet" );
    
    /** <p>Information related to an International Address or Label</p> */
    public static final OntClass Intl = m_model.createClass( "http://www.w3.org/2006/vcard/ns#Intl" );
    
    /** <p>Resources that are vCard Labels</p> */
    public static final OntClass Label = m_model.createClass( "http://www.w3.org/2006/vcard/ns#Label" );
    
    /** <p>Resources that are vCard geographic locations</p> */
    public static final OntClass Location = m_model.createClass( "http://www.w3.org/2006/vcard/ns#Location" );
    
    /** <p>Modem Communications</p> */
    public static final OntClass Modem = m_model.createClass( "http://www.w3.org/2006/vcard/ns#Modem" );
    
    /** <p>Voice Message Communications</p> */
    public static final OntClass Msg = m_model.createClass( "http://www.w3.org/2006/vcard/ns#Msg" );
    
    /** <p>Resources that are vCard personal names</p> */
    public static final OntClass Name = m_model.createClass( "http://www.w3.org/2006/vcard/ns#Name" );
    
    /** <p>Resources that are vCard organizations</p> */
    public static final OntClass Organization = m_model.createClass( "http://www.w3.org/2006/vcard/ns#Organization" );
    
    /** <p>Personal Communications Service</p> */
    public static final OntClass PCS = m_model.createClass( "http://www.w3.org/2006/vcard/ns#PCS" );
    
    /** <p>Pager Communications</p> */
    public static final OntClass Pager = m_model.createClass( "http://www.w3.org/2006/vcard/ns#Pager" );
    
    /** <p>Information related to a Parcel Address or Label</p> */
    public static final OntClass Parcel = m_model.createClass( "http://www.w3.org/2006/vcard/ns#Parcel" );
    
    /** <p>Information related to a Postal Address or Label</p> */
    public static final OntClass Postal = m_model.createClass( "http://www.w3.org/2006/vcard/ns#Postal" );
    
    /** <p>Information related to a Preferred Address, Email, Label, or Telephone</p> */
    public static final OntClass Pref = m_model.createClass( "http://www.w3.org/2006/vcard/ns#Pref" );
    
    /** <p>TelephoneResources that are vCard Telephony communication mechanisms</p> */
    public static final OntClass Tel = m_model.createClass( "http://www.w3.org/2006/vcard/ns#Tel" );
    
    /** <p>Resources that are vCards and the URIs that denote these vCards can also be 
     *  the same URIs that denote people/orgs</p>
     */
    public static final OntClass VCard = m_model.createClass( "http://www.w3.org/2006/vcard/ns#VCard" );
    
    /** <p>Video Communications</p> */
    public static final OntClass Video = m_model.createClass( "http://www.w3.org/2006/vcard/ns#Video" );
    
    /** <p>Voice Communications</p> */
    public static final OntClass Voice = m_model.createClass( "http://www.w3.org/2006/vcard/ns#Voice" );
    
    /** <p>Information related to a Work Address, Label, or Telephone</p> */
    public static final OntClass Work = m_model.createClass( "http://www.w3.org/2006/vcard/ns#Work" );
    
    /** <p>X.400 Email</p> */
    public static final OntClass X400 = m_model.createClass( "http://www.w3.org/2006/vcard/ns#X400" );
    
}
