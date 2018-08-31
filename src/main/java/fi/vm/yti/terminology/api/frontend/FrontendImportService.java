package fi.vm.yti.terminology.api.frontend;

import com.fasterxml.jackson.core.io.JsonStringEncoder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.vm.yti.security.AuthenticatedUserProvider;
import fi.vm.yti.security.YtiUser;
import fi.vm.yti.terminology.api.TermedRequester;
import fi.vm.yti.terminology.api.exception.NodeNotFoundException;
import fi.vm.yti.terminology.api.model.ntrf.*;
import fi.vm.yti.terminology.api.model.termed.*;
import fi.vm.yti.terminology.api.security.AuthorizationManager;
import fi.vm.yti.terminology.api.util.JsonUtils;
import fi.vm.yti.terminology.api.util.Parameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.JAXBElement;
import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static fi.vm.yti.security.AuthorizationException.check;
import static fi.vm.yti.terminology.api.model.termed.VocabularyNodeType.TerminologicalVocabulary;
import static fi.vm.yti.terminology.api.model.termed.VocabularyNodeType.Vocabulary;
import static fi.vm.yti.terminology.api.util.CollectionUtils.mapToList;
import static java.util.Arrays.asList;
import static java.util.Arrays.deepHashCode;
import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toMap;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class FrontendImportService {

    private static final String USER_PASSWORD = "user";
    private static final Pattern UUID_PATTERN = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    private final TermedRequester termedRequester;
    private final FrontendGroupManagementService groupManagementService;
    private final FrontendTermedService termedService;
    private final AuthenticatedUserProvider userProvider;
    private final AuthorizationManager authorizationManager;
    private final String namespaceRoot;

    /**
     * Map containing metadata types. usef  when creating nodes.
     */
    private HashMap<String,MetaNode> typeMap = new HashMap<>();
    /**
     * Map containing node.code or node.uri as a key and  UUID as a value. Used for matching existing items and updating
     * them instead of creating new ones
     */
    private HashMap<String,UUID> idMap = new HashMap<>();

    private static final Logger logger = LoggerFactory.getLogger(FrontendImportService.class);

    @Autowired
    public FrontendImportService(TermedRequester termedRequester,
                                 FrontendGroupManagementService groupManagementService,
                                 FrontendTermedService frontendTermedService,
                                 AuthenticatedUserProvider userProvider,
                                 AuthorizationManager authorizationManager,
                                 @Value("${namespace.root}") String namespaceRoot) {
        this.termedRequester = termedRequester;
        this.groupManagementService = groupManagementService;
        this.termedService = frontendTermedService;
        this.userProvider = userProvider;
        this.authorizationManager = authorizationManager;
        this.namespaceRoot = namespaceRoot;
    }


    ResponseEntity handleNtrfDocument(String format, UUID vocabularityId, VOCABULARYType ntrfDocument) {
        System.out.println("POST /import requested with format:"+format+" VocId:"+vocabularityId);
        Long startTime = new Date().getTime();
        // Fail if given format string is not ntrf
        if (!format.equals("ntrf")) {
            // Unsupported format
            return new ResponseEntity<>("Unsupported format:<" + format + ">    (Currently supported formats: ntrf)\n", HttpStatus.NOT_ACCEPTABLE);
        }
        // Get vocabularity
        Graph vocabularity = termedService.getGraph(vocabularityId);

        initImport(vocabularityId);

        // Get statistic of terms
        List<?> l = ntrfDocument.getHEADEROrDIAGOrHEAD();
        System.out.println("Incoming objects count=" + l.size());
        // Get all records (mapped to terms) from incoming ntrf-document. Check object type and typecast matching objects to  list<>
        List<RECORDType> records = l.stream().filter(o -> o instanceof RECORDType).map(o -> (RECORDType) o).collect(Collectors.toList());
        System.out.println("Incoming records count=" + records.size());

        List<GenericNode> addNodeList = new ArrayList<>();

        int flushCount = 0;
        for(RECORDType o:records){
            handleRecord(vocabularity, o, addNodeList);
            flushCount++;
            if(flushCount >100){
                flushCount=0;
                GenericDeleteAndSave operation = new GenericDeleteAndSave(emptyList(),addNodeList);
                System.out.println("------------------------------------");
//                JsonUtils.prettyPrintJson(operation);
                System.out.println("------------------------------------");
                termedService.bulkChange(operation,true);
                addNodeList.clear();
            }
        }
        /*
        records.forEach(o -> {
                handleRecord(vocabularity, o, addNodeList);
        });
        */
        GenericDeleteAndSave operation = new GenericDeleteAndSave(emptyList(),addNodeList);
        System.out.println("------------------------------------");
        JsonUtils.prettyPrintJson(operation);
        System.out.println("------------------------------------");
        termedService.bulkChange(operation,true);

        Long endTime = new Date().getTime();

        System.out.println("Operation  took "+(endTime-startTime)/1000+"s");
        return new ResponseEntity<>("Imported "+records.size()+" terms using format:<" + format + ">\n", HttpStatus.OK);
    }

    private void initImport(UUID vocabularityId){
        // Get metamodel types for given vocabularity
        List<MetaNode> metaTypes = termedService.getTypes(vocabularityId);
        metaTypes.forEach(t-> {
            System.out.println("Adding "+t.getId());
            typeMap.put(t.getId(),t);
        });
        System.out.println("Metamodel types found: "+metaTypes.size());
        System.out.println("get node ids for update");

        // Create hashmap to store information  between code/URI and UUID so that we can update values upon same vocabularity
        List<GenericNode> nodeList = termedService.getNodes(vocabularityId);
        nodeList.forEach(o->{
            System.out.println(" Code:"+o.getCode() +" UUID:"+o.getId().toString()+" URI:"+o.getUri());
            if(!o.getCode().isEmpty()){
                idMap.put(o.getCode(),o.getId());
            }
            if(!o.getUri().isEmpty()){
                idMap.put(o.getUri(),o.getId());
            }
        });
    }

    /**
     * Handle mapping of RECORD
     * incomimg NTRF:
     *   <RECORD numb="tmpOKSAID116" upda="Riina Kosunen, 2018-03-16">
     *     <LANG value="fi">
     *       <TE>
     *         <TERM>kasvatus</TERM>
     *         <SOURF>SRemes</SOURF>
     *       </TE>
     *       <DEF>vuorovaikutukseen perustuva toiminta, jonka tavoitteena on kehittää yksilöstä eettisesti vastuukykyinen yhteiskunnan jäsen<SOURF>wikipedia + rk + pikkuryhma_01 + tr45 + tr49 + ssu + vaka_tr_01 + vaka_tr_02 + tr63</SOURF></DEF>
     *       <NOTE>Kasvatuksen myötä kulttuuriset arvot, tavat ja normit välittyvät ja muovautuvat. Osaltaan kasvatuksen tavoite on siirtää kulttuuriperintöä sekä tärkeinä pidettyjä arvoja ja traditioita seuraavalle sukupolvelle, mutta kasvatuksen avulla halutaan myös uudistaa ajattelu- ja toimintatapoja. Kasvatuksen sivistystehtävänä on tietoisesti ohjata yksilöllisen identiteetin muotoutumista ja huolehtia, että muotoutuminen tapahtuu sosiaalisesti hyväksyttävällä tavalla.<SOURF>vaka_tr_02 + tr63</SOURF></NOTE>
     *       <NOTE>Varhaiskasvatuksella tarkoitetaan varhaiskasvatuslain (<LINK href="https://www.finlex.fi/fi/laki/ajantasa/1973/19730036">36/1973</LINK>) mukaan lapsen suunnitelmallista ja tavoitteellista kasvatuksen, <RCON href="#tmpOKSAID117">opetuksen (1)</RCON> ja hoidon muodostamaa kokonaisuutta, jossa painottuu pedagogiikka.<SOURF>vk-peruste + ssu + vaka_tr_02</SOURF></NOTE>
     *       <NOTE><RCON href="#tmpOKSAID452">Perusopetuksella</RCON> on opetustavoitteiden lisäksi kasvatustavoitteita. Perusopetuslain (<LINK href="https://www.finlex.fi/fi/laki/ajantasa/1998/19980628">628/1998</LINK>) mukaan perusopetuksella pyritään kasvattamaan oppilaita ihmisyyteen ja eettisesti vastuukykyiseen yhteiskunnan jäsenyyteen sekä antamaan heille elämässä tarpeellisia tietoja ja taitoja.<SOURF>rk + pikkuryhma_01 + tr45</SOURF></NOTE>
     *       <NOTE>Yliopistolain (<LINK href="https://www.finlex.fi/fi/laki/ajantasa/2009/20090558">558/2009</LINK>) mukaan <RCON href="#tmpOKSAID162">yliopistojen</RCON> tehtävänä on edistää vapaata tutkimusta sekä tieteellistä ja taiteellista sivistystä, antaa tutkimukseen perustuvaa ylintä opetusta (1) sekä kasvattaa <RCON href="#tmpOKSAID227">opiskelijoita</RCON> palvelemaan isänmaata ja ihmiskuntaa.<SOURF>558/2009 + tr45</SOURF></NOTE>
     *       <NOTE>Englannin käsite education on laajempi kuin suomen kasvatus, niin että termillä education viitataan kasvatuksen lisäksi muun muassa <RCON href="#tmpOKSAID117">opetukseen (1)</RCON>, <RCON href="#tmpOKSAID121">koulutukseen (1)</RCON> ja sivistykseen.<SOURF>rk + KatriSeppala</SOURF></NOTE>
     *       <NOTE>Käsitteen tunnus: tmpOKSAID116</NOTE>
     *     </LANG>
     *     <LANG value="sv">
     *       <TE>
     *         <TERM>fostran</TERM>
     *         <SOURF>36/1973_sv + kielityoryhma_sv_04</SOURF>
     *       </TE>
     *     </LANG>
     *     <LANG value="en">
     *       <TE>
     *         <EQUI value="broader"></EQUI>
     *         <TERM>education</TERM>
     *         <HOGR>1</HOGR>
     *         <SOURF>ophall_sanasto</SOURF>
     *       </TE>
     *       <SY>
     *         <TERM>upbringing</TERM>
     *         <SOURF>MOT_englanti</SOURF>
     *       </SY>
     *     </LANG>
     *     <CLAS>yleinen/yhteinen</CLAS>
     *     <CHECK>hyväksytty</CHECK>
     *   </RECORD>
     * @param vocabularity Graph-node from termed. It contains base-uri where esck Concept and Term is bound
     * @param r
     */
    void handleRecord(Graph vocabularity, RECORDType r, List<GenericNode> addNodeList) {
        String code="";
        String uri="";
        Long number= 0L;
        String createdBy;
        LocalDate createdDate;
        String lastModifiedBy = null;
        LocalDate lastModifiedDate= LocalDate.now();
        TypeId type;

        code = r.getNumb();
        // Derfault  creator is importing user
        createdBy = userProvider.getUser().getUsername();

        logger.info("Record id:"+code);
        if(r.getName() != null)
            System.out.print(" Name="+r.getName());
        if(r.getStat() != null)
            System.out.print(" stat="+r.getStat());
        if(r.getUpda() != null) {
            // Store that information to the  modificationHistory
            String updater=r.getUpda();
            String upd[]=r.getUpda().split(",");
            if(upd.length == 2) {
                DateTimeFormatter df =  DateTimeFormatter.ofPattern("yyyy-MM-dd");
                // User,  update date
                lastModifiedBy = upd[0].trim();
                try {
                    lastModifiedDate = LocalDate.parse(upd[1].trim(), df);
                    System.out.println("LastModifiedBy:"+lastModifiedBy+"  LastModifDate="+lastModifiedDate);
                } catch(DateTimeParseException dex){
                    System.out.println("Parse error for date"+dex.getMessage());
                }
            }
        }

        // Print content
        List<?> l = r.getContent();
        // Filter all JAXBElements
        List<JAXBElement> elems=l.stream().filter(o -> o instanceof JAXBElement).map(o -> (JAXBElement)o).collect(Collectors.toList());
        elems.forEach(o -> {
            System.out.println(" Found ELEM: " + o.getName().toString());
        });
        // Attributes  are stored to property-list
        Map<String, List<Attribute>> properties = new HashMap<>();

        // Resolve terms and  collect list of them for insert.
        List<GenericNode> terms = new ArrayList<>();
        // Filter LANG elemets as list.
        List<LANGType> langs = elems.stream().filter(o -> o.getName().toString().equals("LANG")).map(o -> (LANGType)o.getValue()).collect(Collectors.toList());
        langs.forEach(o -> {
            // RECORD/LANG/TE/TERM -> prefLabel
            terms.add(hadleLang(o, properties, vocabularity));
        });
        // Filter CHECK elemets as list
        List<String> check = elems.stream().filter(o -> o.getName().toString().equals("CHECK")).map(o -> (String)o.getValue()).collect(Collectors.toList());
        check.forEach(o -> {
            System.out.println("--CHECK=" + o);
            //RECORD/CHECK ->
            handleStatus(o, properties);
        });

        TypeId typeId = typeMap.get("Concept").getDomain();
        GenericNode node = null;
        // Check if we update concept
        if(idMap.get(code) != null){
            System.out.println(" UPDATE operation!!!!!!!!!");
            node = new GenericNode(idMap.get(code),code, vocabularity.getUri() + code, 0L, createdBy, new Date(), "", new Date(), typeId, properties, emptyMap(), emptyMap());
        } else {
            node = new GenericNode(code, vocabularity.getUri() + code, 0L, createdBy, new Date(), "", new Date(), typeId, properties, emptyMap(), emptyMap());
            System.out.println(" CREATE NEW operation!!!!!!");
        }
        // Send item to termed-api
        // First add terms
        terms.forEach(t->{addNodeList.add(t);});
        // then concept itself
        addNodeList.add(node);

        /*
        GenericDeleteAndSave operation = new GenericDeleteAndSave(emptyList(),addedNodes);
        System.out.println("------------------------------------");
        JsonUtils.prettyPrintJson(operation);
        System.out.println("------------------------------------");
        termedService.bulkChange(operation,true);
        return addedNodes;
        */
    }

    /**
     * Handle mapping of individual Lang-element. This contains Terms which needs to be created and definition and some other information which
     * should be stored under parent concept
     * prefLabel value is found under LANG/TE/TERM
     * parent.definition is  under LANG/TE/DEF
     * Incoming NTRF:
     *     <LANG value="fi">
     *       <TE>
     *         <TERM>opetus</TERM>
     *         <HOGR>1</HOGR>
     *         <SOURF>harmon + tr45</SOURF>
     *       </TE>
     *       <DEF>vuorovaikutukseen perustuva toiminta, jonka tavoitteena on <RCON href="#tmpOKSAID118">oppiminen</RCON><SOURF>wikipedia + rk + tr45</SOURF></DEF>
     *       <NOTE>Opetuksella (1) ja <RCON href="#tmpOKSAID116">kasvatuksella</RCON> on osin yhteneväisiä tavoitteita.<SOURF>vaka_tr_02 + tr63</SOURF></NOTE>
     *       <NOTE>Englannin käsite education on laajempi kuin suomen opetus (1), niin että termillä education viitataan opetuksen (1) lisäksi muun muassa <RCON href="#tmpOKSAID116">kasvatukseen</RCON>, <RCON href="#tmpOKSAID121">koulutukseen (1)</RCON> ja sivistykseen.<SOURF>rk + KatriSeppala</SOURF></NOTE>
     *       <NOTE>Käsitteen tunnus: tmpOKSAID117</NOTE>
     *     </LANG>
     * @param o LANGType containing incoming NTRF-block
     * @param vocabularity Graph-element containing  information of  parent vocabularity like id and base-uri
     */
    private GenericNode hadleLang(LANGType o, Map<String, List<Attribute>> parentProperties, Graph vocabularity)  {
         // generate random UUID as a code and use it as part if the generated URI
         String code = UUID.randomUUID().toString();

        logger.info("Handle LANG:"+ o.getValueAttribute());

        // Attributes  are stored to property-list
        Map<String, List<Attribute>> properties = new HashMap<>();
        List<?> e=o.getTEOrDEFOrNOTE();
        // Filter TE elemets as list and add mapped elements as properties under node
        List<TEType> terms = e.stream().filter(t -> t instanceof TEType).map(t -> (TEType)t).collect(Collectors.toList());
        for(TEType i:terms){
            logger.info("Handle Term:"+i.getTERM().getContent().toString());
            List<Serializable> li = i.getTERM().getContent();
            if(!li.isEmpty()) { // if value exist
//                String value = i.getTERM().getContent().get(0).toString();
                String value = getAttributeContent(i.getTERM().getContent());
                Attribute att = new Attribute(o.getValueAttribute(), value);
                addProperty("prefLabel", properties,  att);
            }

        };
        //DEFINITION
        List<DEFType> def = e.stream().filter(t -> t instanceof DEFType).map(t -> (DEFType)t).collect(Collectors.toList());
        // Definition is complex multi-line object which needs to be resolved
        for(DEFType d:def){
            System.out.println(" Found Definition: " + d.getContent().toString());
            handleDEF(d, o.getValueAttribute(), parentProperties,vocabularity);
        }

        // NOTE
        List<NOTEType> notes = e.stream().filter(t -> t instanceof NOTEType).map(t -> (NOTEType)t).collect(Collectors.toList());
        for(NOTEType n:notes){
            System.out.println(" Found Definition: " + n.getContent().toString());
            handleNOTE(n, o.getValueAttribute(), parentProperties,vocabularity);
        }

        TypeId typeId = typeMap.get("Term").getDomain();
        // Uri is  parent-uri/term-'code'
        GenericNode node = null;
        if(idMap.get(code) != null) {
            logger.debug("Update Term");
            System.out.println("Update TERM!!!! ");
            node = new GenericNode(idMap.get(code),code, vocabularity.getUri() + "term-" + code, 0L, "", new Date(), "", new Date(), typeId, properties, emptyMap(), emptyMap());
        }
        else {
            logger.debug("Create Term");
            node = new GenericNode(code, vocabularity.getUri() + "term-" + code, 0L, "", new Date(), "", new Date(), typeId, properties, emptyMap(), emptyMap());
        }
        return node;
    }

    private String getAttributeContent( List<Serializable> li) {
        String value = null;
        if (!li.isEmpty()) { // if value exist
            value = li.get(0).toString();
        }
        return value;
    }

    /**
     * Handle CHECK->status-property mapping
     * @param o CHECK-field
     * @param properties Propertylist where status is added
     */
    private Attribute  handleStatus( String o, Map<String, List<Attribute>> properties){
        System.out.println(" Set status: " + o);
        String stat = "DRAFT";
        /*
           keskeneräinen       | 'INCOMPLETE'
           korvattu            | 'SUPERSEDED'
           odottaa hyväksyntää | 'SUBMITTED'
                               | 'RETIRED'
                               | 'INVALID'
           hyväksytty          | 'VALID'
                               | 'SUGGESTED'
           luonnos             | 'DRAFT'
         */
        if(o.equals("hyväksytty"))
            stat = "VALID";
        Attribute att = new Attribute("", stat);
        addProperty("status", properties, att);
        return att;
    }

    private Attribute  handleDEF( DEFType def, String lang, Map<String, List<Attribute>>  parentProperties, Graph vocabularity){
        logger.debug("handleDEF-part:"+def.getContent());
        String defString="";

        List<Serializable> defItems = def.getContent();
        for(Serializable de:defItems) {
            if(de instanceof  String) {
                defString =defString.concat(de.toString());
            }
            else {
                if(de instanceof JAXBElement){
                    JAXBElement j = (JAXBElement)de;
                    if(j.getName().toString().equalsIgnoreCase("RCON")){
                        // <NCON href="#tmpOKSAID122" typr="partitive">koulutuksesta (2)</NCON> ->
                        // <a href="http://uri.suomi.fi/terminology/oksa/tmpOKSAID122" data-typr="partitive">koulutuksesta (2)</a>
                        // <DEF>suomalaista <RCON href="#tmpOKSAID564">ylioppilastutkintoa</RCON> vastaava <RCON href="#tmpOKSAID436">Eurooppa-koulujen</RCON> <BCON href="#tmpOKSAID1401" typr="generic">tutkinto</BCON>, joka suoritetaan kaksivuotisen <RCON href="#tmpOKSAID456">lukiokoulutuksen</RCON> päätteeksi<SOURF>opintoluotsi + rk + tr34</SOURF></DEF>
                        RCONType rc=(RCONType)j.getValue();
                        defString = defString.concat("<a href='"+
                                vocabularity.getUri());
                        // Remove # from uri
                        if(rc.getHref().startsWith("#")) {
                            defString = defString.concat(rc.getHref().substring(1) + "'");
                        } else
                            defString = defString.concat(rc.getHref() + "'");
                        System.out.println("TYPER="+rc.getTypr());
                        if(rc.getTypr() != null && !rc.getTypr().isEmpty()) {
                            defString = defString.concat(" data-typr ='" +
                                    rc.getTypr()+"'");
                        }
                        defString = defString.concat(">"+rc.getContent().get(0)+ "</a>");
                    }
                    else if(j.getName().toString().equalsIgnoreCase("SOURF")) {
                        DEFType.SOURF sf = (DEFType.SOURF)j.getValue();
                        if(sf.getContent()!= null && sf.getContent().size() >0) {
                            System.out.println("-----SOURF=" + sf.getContent());
                            defString = defString.concat(" "+sf.getContent());
                        }
                    } else
                        System.out.println("  def-class" + de.getClass().getName());
                }
                logger.info("Definition="+defString);
            }
        };
        Attribute att = new Attribute(lang, defString);
        addProperty("definition", parentProperties,  att);
        return att;
    }

    private Attribute  handleNOTE( NOTEType note, String lang, Map<String, List<Attribute>>  parentProperties, Graph vocabularity){
        logger.debug("handleNOTE-part"+note.getContent());

        String noteString="";

        List<Serializable> noteItems = note.getContent();
        for(Serializable de:noteItems) {
            if(de instanceof  String) {
                System.out.println("  note-string"+de.toString());
                noteString =noteString.concat(de.toString());
            }
            else {
                if(de instanceof JAXBElement){
                    JAXBElement j = (JAXBElement)de;
                    System.out.println("  note-elem <" + j.getName()+">");
                    if(j.getName().toString().equalsIgnoreCase("RCON")){
                        System.out.println("RCON------");
                        // <NCON href="#tmpOKSAID122" typr="partitive">koulutuksesta (2)</NCON> ->
                        // <a href="http://uri.suomi.fi/terminology/oksa/tmpOKSAID122" data-typr="partitive">koulutuksesta (2)</a>
                        // <DEF>suomalaista <RCON href="#tmpOKSAID564">ylioppilastutkintoa</RCON> vastaava <RCON href="#tmpOKSAID436">Eurooppa-koulujen</RCON> <BCON href="#tmpOKSAID1401" typr="generic">tutkinto</BCON>, joka suoritetaan kaksivuotisen <RCON href="#tmpOKSAID456">lukiokoulutuksen</RCON> päätteeksi<SOURF>opintoluotsi + rk + tr34</SOURF></DEF>
                        RCONType rc=(RCONType)j.getValue();
                        System.out.println("RCON-HREF <a href='"+vocabularity.getUri()+rc.getHref()+"' data-typr='" + rc.getTypr() + "'>"+rc.getContent().get(0)+ "</a>");
                        noteString = noteString.concat("<a href='"+
                                vocabularity.getUri());
                        // Remove # from uri
                        if(rc.getHref().startsWith("#")) {
                            noteString = noteString.concat(rc.getHref().substring(1) + "'");
                        } else
                            noteString = noteString.concat(rc.getHref() + "'");
                        System.out.println("TYPER="+rc.getTypr());
                        if(rc.getTypr() != null && !rc.getTypr().isEmpty()) {
                            noteString = noteString.concat(" data-typr ='" +
                                    rc.getTypr()+"'");
                        }
                        noteString = noteString.concat(">"+rc.getContent().get(0)+ "</a>");
                    }
                    else if(j.getName().toString().equalsIgnoreCase("SOURF")) {
                        NOTEType.SOURF sf = (NOTEType.SOURF)j.getValue();
                        if(sf.getContent()!= null && sf.getContent().size() >0) {
                            System.out.println("-----SOURF=" + sf.getContent());
                            noteString = noteString.concat(" "+sf.getContent());
                        }
                    } else
                        System.out.println("  note-class" + de.getClass().getName());
                }
                System.out.println("  note-String=" + noteString);
            }
        };
        Attribute att = new Attribute(lang, noteString);
        addProperty("note", parentProperties,  att);
        return att;
    }

    /**
     * Add individual named attribute to property list
     * @param attributeName like prefLabel
     * @param properties  Propertylist where attribute is added
     * @param att Attribute to be added
     */
    private void addProperty(String attributeName, Map<String, List<Attribute>> properties, Attribute att){
        if (!properties.containsKey(attributeName)) {
            List<Attribute> a = new ArrayList<>();
            a.add(att);
            properties.put(attributeName, a);
        } else
            properties.get(attributeName).add(att);
    }

    private static <K, V> Map<K, List<V>> mapMapValues(Map<K, List<V>> map, Function<V, V> mapper) {
        return map.entrySet().stream().collect(toMap(Map.Entry::getKey, e -> mapToList(e.getValue(), mapper)));
    }

    private static boolean isUUID(String s) {
        return UUID_PATTERN.matcher(s).matches();
    }

}
