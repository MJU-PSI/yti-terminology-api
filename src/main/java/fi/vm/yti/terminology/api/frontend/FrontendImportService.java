package fi.vm.yti.terminology.api.frontend;

import fi.vm.yti.security.AuthenticatedUserProvider;
import fi.vm.yti.terminology.api.TermedRequester;
import fi.vm.yti.terminology.api.model.ntrf.*;
import fi.vm.yti.terminology.api.model.termed.*;
import fi.vm.yti.terminology.api.security.AuthorizationManager;
import fi.vm.yti.terminology.api.util.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.xml.bind.JAXBElement;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static fi.vm.yti.security.AuthorizationException.check;
import static fi.vm.yti.terminology.api.util.CollectionUtils.mapToList;
import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class FrontendImportService {

    private static final String USER_PASSWORD = "user";
    private static final Pattern UUID_PATTERN = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    private final UUID NULL_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    private final TermedRequester termedRequester;
    private final FrontendGroupManagementService groupManagementService;
    private final FrontendTermedService termedService;
    private final AuthenticatedUserProvider userProvider;
    private final AuthorizationManager authorizationManager;
    private final String namespaceRoot;


    /**
     * Map containing metadata types. used  when creating nodes.
     */
    private HashMap<String,MetaNode> typeMap = new HashMap<>();
    /**
     * Map containing node.code or node.uri as a key and  UUID as a value. Used for matching existing items and updating
     * them instead of creating new ones
     */
    private HashMap<String,UUID> idMap = new HashMap<>();
    /**
     * Map containing node.code or node.uri as a key and  UUID as a value. Used for reference resolving after all
     * concepts and terms are created
     */
    private HashMap<String,UUID> createdIdMap = new HashMap<>();

    /**
     * Map binding together reference string and external URL fromn ntrf SOURF-element
     */
    private HashMap<String,HashMap<String,String>> referenceMap = new HashMap<>();

    /**
     * Map for NCON-reference cache. Operation targetId, type(generic/partitive), broaderConceptId
     */
    private List<NconRef> nconList = new ArrayList<>();

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

    /**
     * Executes import operation. Reads incoming xml and process it
     * @param format
     * @param vocabularityId
     * @param ntrfDocument
     * @return
     */
    ResponseEntity handleNtrfDocument(String format, UUID vocabularityId, VOCABULARYType ntrfDocument) {
        Graph vocabularity = null;
        logger.info("POST /import requested with format:"+format+" Vocabularity Id:"+vocabularityId);
        Long startTime = new Date().getTime();
        // Fail if given format string is not ntrf
        if (!format.equals("ntrf")) {
            // Unsupported format
            return new ResponseEntity<>("Unsupported format:<" + format + ">    (Currently supported formats: ntrf)\n", HttpStatus.NOT_ACCEPTABLE);
        }

        // Get vocabularity
        try {
            vocabularity = termedService.getGraph(vocabularityId);
        } catch ( NullPointerException nex){
            // Vocabularity not found
            return new ResponseEntity<>("Vocabularity:<" + vocabularityId + "> not found\n", HttpStatus.NOT_FOUND);
        }

        initImport(vocabularityId);

        // Get statistic of terms
        List<?> l = ntrfDocument.getHEADEROrDIAGOrHEAD();
        logger.info("Incoming objects count=" + l.size());

        // Get all reference-elements and build reference-url-map
        List<REFERENCESType> externalReferences = l.stream().filter(o -> o instanceof REFERENCESType).map(o -> (REFERENCESType) o).collect(Collectors.toList());
        handleReferences(externalReferences,referenceMap);
        logger.info("Incoming reference count=" + externalReferences.size());

        // Get all records (mapped to terms) from incoming ntrf-document. Check object type and typecast matching objects to  list<>
        List<RECORDType> records = l.stream().filter(o -> o instanceof RECORDType).map(o -> (RECORDType) o).collect(Collectors.toList());
        logger.info("Incoming records count=" + records.size());

        List<GenericNode> addNodeList = new ArrayList<>();

        int flushCount = 0;
        for(RECORDType o:records){
            handleRECORD(vocabularity, o, addNodeList);
            flushCount++;
            if(flushCount >200){
                flushCount=0;
                GenericDeleteAndSave operation = new GenericDeleteAndSave(emptyList(),addNodeList);
                if(logger.isDebugEnabled())
                    logger.debug(JsonUtils.prettyPrintJsonAsString(operation));
                termedService.bulkChange(operation,true);
                addNodeList.clear();
            }
        }
        GenericDeleteAndSave operation = new GenericDeleteAndSave(emptyList(),addNodeList);
        if(logger.isDebugEnabled())
            logger.debug(JsonUtils.prettyPrintJsonAsString(operation));
        termedService.bulkChange(operation,true);
        addNodeList.clear();

        Long endTime = new Date().getTime();

        // ReInitialize caches and after that, resolve ncon-references
        idMap.clear();
        typeMap.clear();
        initImport(vocabularityId);
        nconList.forEach(nref ->{
            if(nref.getId() ==  NULL_ID){
                UUID target=idMap.get(nref.referenceString);
                System.out.println("Resolving originally unresolved id:"+nref.referenceString);
                // Update it
                nref.setId(target);
            }
            GenericNode gn = termedService.getConceptNode(vocabularityId,nref.getId());
            if(gn!= null){
                // get objects reference list and add
                Map<String, List<Identifier>> refMap = gn.getReferences();
                List<Identifier> ref = null;
                if(nref.getType().equalsIgnoreCase("generic")){
                    ref = refMap.get("broader");
                } else
                    ref = refMap.get("isPartOf");
                if (ref == null)
                    ref = new ArrayList<>();
                // @TODO! Go through refList and add only if missing.
                // Generic = broader concept, partitive = related concept
                ref.add(new Identifier(nref.getTargetId(), typeMap.get("Concept").getDomain()));
                if(nref.getType().equalsIgnoreCase("generic")) {
                    refMap.put("broader", ref);
                } else
                    refMap.put("isPartOf", ref);
            }
            addNodeList.add(gn);
        });
        // add NCON-changes as one big block
        operation = new GenericDeleteAndSave(emptyList(),addNodeList);
        if(logger.isDebugEnabled())
            logger.debug(JsonUtils.prettyPrintJsonAsString(operation));
        termedService.bulkChange(operation,true);

        // Handle DIAG-elements and create collections from them
        List<DIAGType> diagTypeList = l.stream().filter(o -> o instanceof DIAGType).map(o -> (DIAGType) o).collect(Collectors.toList());
        for(DIAGType o:diagTypeList) {
            handleDIAG(vocabularity, o, addNodeList);
        }
        // Add DIAG-list to vocabularity
        operation = new GenericDeleteAndSave(emptyList(),addNodeList);
        if(logger.isDebugEnabled())
            logger.debug(JsonUtils.prettyPrintJsonAsString(operation));
        termedService.bulkChange(operation,true);
        System.out.println("Operation  took "+(endTime-startTime)/1000+"s");
        return new ResponseEntity<>("Imported "+records.size()+" terms using format:<" + format + ">\n", HttpStatus.OK);
    }

    /**
     * Initialize importer.
     * - Read given vocabularity for meta-types, cache them
     * - Read all existing nodes and cache their URI/UUID-values
     * @param vocabularityId UUID of the vocabularity
     */
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
            if(logger.isDebugEnabled())
                logger.debug(" Code:"+o.getCode() +" UUID:"+o.getId().toString()+" URI:"+o.getUri());
            if(!o.getCode().isEmpty()){
                idMap.put(o.getCode(),o.getId());
            }
            if(!o.getUri().isEmpty()){
                idMap.put(o.getUri(),o.getId());
            }
        });
    }

    private void handleDIAG(Graph vocabularity, DIAGType diag, List<GenericNode> addNodeList){
        String code = diag.getNumb();
        if(logger.isDebugEnabled())
            logger.debug("DIAG Name="+diag.getName()+" Code="+code);

        UUID collectionId=idMap.get(code);
        // Generate new if not update
        if(collectionId == null)
            collectionId = UUID.randomUUID();
        // references list for colection member concepts
        Map<String, List<Identifier>> references = new HashMap<>();
        // Construct empty member list
        List<Identifier> memberRef = new ArrayList<>();
        // Construct properties map and add name as preflabel
        Map<String, List<Attribute>> properties = new HashMap<>();
        // Default lang is finnish at the time
        addProperty("prefLabel", properties, new Attribute("fi", diag.getName()));

        // get links and add them to references/member map
        List<?> l = diag.getContent();
        // Filter all JAXBElements
        List<JAXBElement> elems=l.stream().filter(o -> o instanceof JAXBElement).map(o -> (JAXBElement)o).collect(Collectors.toList());
        elems.forEach(o -> {
            if (o instanceof JAXBElement) {
                JAXBElement j = (JAXBElement) o;
                if (j.getName().toString().equalsIgnoreCase("LINK")) {
                    LINKType lt = (LINKType)j.getValue();
                    String linkTarget = lt.getHref();
                    // Remove #
                    if (linkTarget.startsWith("#"))
                        linkTarget = linkTarget.substring(1);
                    UUID targetUUID = idMap.get(linkTarget);
                    if(targetUUID == null){
                        System.out.println("Warning! Can't resolve LINK "+ linkTarget+" target UUID.");
                    }
                    memberRef.add(new Identifier(targetUUID, typeMap.get("Concept").getDomain()));
                    references.put("member", memberRef);
                }
            }
        });
        // Construct Node as Collection-type
        GenericNode node = new GenericNode(collectionId, code, vocabularity.getUri() + code, 0L, userProvider.getUser().getUsername(), new Date(), "", new Date(), typeMap.get("Collection").getDomain(), properties, references, emptyMap());
        // Just add it
        addNodeList.add(node);
    }

    /**
     * TSK-NTRF packs external references as REFERENCES-items. Here we go through them and cache
     * values for later usage
     * @param referencesTypeList
     * @param refMap
     */
    private void handleReferences(List<REFERENCESType>referencesTypeList, HashMap<String,HashMap<String,String>> refMap){
        referencesTypeList.forEach(reftypes -> {
            List<REFType> refs = reftypes.getREFOrREFHEAD().stream().filter(o -> o instanceof REFType).map(o -> (REFType) o).collect(Collectors.toList());
            refs.forEach(r -> {
                // Filter all JAXBElements
                List<JAXBElement> elems = r.getContent().stream().filter(o -> o instanceof JAXBElement).map(o -> (JAXBElement) o).collect(Collectors.toList());
                HashMap<String, String> fields = new HashMap<>();
                String name = "";
                for(JAXBElement o:elems){
                    String text = null;
                    String url = null;
                    if (o.getName().toString().equalsIgnoreCase("REFNAME")) {
                        name = o.getValue().toString();
                    }
                    if (o.getName().toString().equalsIgnoreCase("REFTEXT")) {
                        REFType.REFTEXT rt = (REFType.REFTEXT) o.getValue();
                        text = rt.getContent().toString();
                        fields.put("text", text);
                    }
                    if (o.getName().toString().equalsIgnoreCase("REFLINK")) {
                        if (!o.getValue().toString().isEmpty()) {
                            url = o.getValue().toString();
                            fields.put("url", url);
                        }
                    }
                    if(logger.isDebugEnabled())
                        logger.debug(" Cache incoming external references: field=" +fields);
                };
                // add fields ro referenceMap
                // for instance
                if(name != null) {
                    System.out.println("PUT REFMAP id:" + name + " value:" + fields);
                    refMap.put(name, fields);
                }
            });
        });
    }

    /**
     * Handle mapping of RECORD. NTRF-models all concepts as RECORD-fields. It contains actual terms and references
     * to other concepts and external links. See following example
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
     *     <BCON href="#tmpOKSAID122" typr="generic">koulutus (2)</BCON>
     *     <NCON href="#tmpOKSAID123" typr="generic">koulutuksen toteutus</NCON>
     *     <CLAS>yleinen/yhteinen</CLAS>
     *     <CHECK>hyväksytty</CHECK>
     *   </RECORD>
     * @param vocabularity Graph-node from termed. It contains base-uri where esck Concept and Term is bound
     * @param r
     */
    void handleRECORD(Graph vocabularity, RECORDType r, List<GenericNode> addNodeList) {
        String code="";
        UUID currentId=null;
        String uri="";
        Long number= 0L;
        String createdBy;
        LocalDate createdDate;
        String lastModifiedBy = null;
        LocalDate lastModifiedDate= LocalDate.now();
        // Attributes  are stored to property-list
        Map<String, List<Attribute>> properties = new HashMap<>();
        // references synomyms and preferred tems and so on
        Map<String, List<Identifier>> references = new HashMap<>();

        code = r.getNumb();
        // Check  whether id exist and create id
        if(idMap.get(code) != null) {
            System.out.println(" UPDATE operation!!!!!!!!! " + code);
            currentId = idMap.get(code);
        } else {
            System.out.println(" CREATE NEW  operation!!!!!!!!! " + code);
            currentId = UUID.randomUUID();
        }

        // Default  creator is importing user
        createdBy = userProvider.getUser().getUsername();

        logger.info("Record id:"+code);
        // Add info to editorial note
        String editorialNote = "";
        if(r.getName() != null)
            editorialNote = editorialNote+r.getName();
        if(r.getStat() != null)
            editorialNote = editorialNote+" "+r.getStat();
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
                    editorialNote = editorialNote+" -"+lastModifiedBy+", "+lastModifiedDate;
                } catch(DateTimeParseException dex){
                    System.out.println("Parse error for date"+dex.getMessage());
                }
            }
        }
        if(!editorialNote.isEmpty()) {
            System.out.println("EDitorial note!!!"+editorialNote);
            Attribute att = new Attribute("fi", editorialNote);
            addProperty("editorialNote", properties, att);
        }
        List<?> l = r.getContent();
        // Filter all JAXBElements
        List<JAXBElement> elems=l.stream().filter(o -> o instanceof JAXBElement).map(o -> (JAXBElement)o).collect(Collectors.toList());
        elems.forEach(o -> {
            System.out.println(" Found ELEM: " + o.getName().toString());
        });


        // Resolve terms and  collect list of them for insert.
        List<GenericNode> terms = new ArrayList<>();
        // Filter LANG elemets as list.
        List<LANGType> langs = elems.stream().filter(o -> o.getName().toString().equals("LANG")).map(o -> (LANGType)o.getValue()).collect(Collectors.toList());
        langs.forEach(o -> {
            // RECORD/LANG/TE/TERM -> prefLabel
            hadleLANG(terms, o, properties, references, vocabularity);
        });
        // Filter CLAS elemets as list
        List<String> clas = elems.stream().filter(o -> o.getName().toString().equals("CLAS")).map(o -> (String)o.getValue()).collect(Collectors.toList());
        clas.forEach(o -> {
            System.out.println("--CLAS=" + o);
            //RECORD/CLAS ->
            handleCLAS(o, properties);
        });
        // Filter CHECK elemets as list
        List<String> check = elems.stream().filter(o -> o.getName().toString().equals("CHECK")).map(o -> (String)o.getValue()).collect(Collectors.toList());
        check.forEach(o -> {
            System.out.println("--CHECK=" + o);
            //RECORD/CHECK ->
            handleCHECK(o, r.getStat(), properties);
        });

        // stat-attribute overrides CHECK
        if(r.getStat()!= null && !r.getStat().isEmpty()){
            handleStat(r.getStat(), properties);
        }
        // Filter BCON elemets as list
        List<BCONType> bcon = elems.stream().filter(o -> o.getName().toString().equals("BCON")).map(o -> (BCONType)o.getValue()).collect(Collectors.toList());
        bcon.forEach(o -> {
            System.out.println("--BCON=" + o.getHref());
            //RECORD/BCON
            handleBCON(o,references);
        });
        // Filter NCON elemets as list
        List<NCONType> ncon = elems.stream().filter(o -> o.getName().toString().equals("NCON")).map(o -> (NCONType)o.getValue()).collect(Collectors.toList());
        for(NCONType o:ncon) {
            System.out.println("--NCON=" + o.getHref());
            String nrefId=o.getHref().substring(1);
            System.out.println("Search Match:"+idMap.get(nrefId));
            System.out.println("Search Match new:"+this.createdIdMap.get(nrefId));
            //RECORD/BCON
            // Store information of link for now on and after all items are created, update broader/isPartOf-references
            handleNCON(o,currentId);
        }

        TypeId typeId = null;
//        if(r.getStat() != null && r.getStat().equalsIgnoreCase("ulottuvuus"))
//            typeId = typeMap.get("Collection").getDomain();
//        else
            typeId = typeMap.get("Concept").getDomain();
        GenericNode node = null;
        node = new GenericNode(currentId, code, vocabularity.getUri() + code, 0L, createdBy, new Date(), "", new Date(), typeId, properties, references, emptyMap());
        // Send item to termed-api
        // First add terms
        terms.forEach(t->{addNodeList.add(t);});
        // then concept itself
        addNodeList.add(node);
        // Add id for reference resolving
        createdIdMap.put(node.getCode(),node.getId());
    }

    /**
     * Handle mapping of individual Lang-element. This contains Terms which needs to be created and definition and some other information which
     * should be stored under parent concept
     * prefLabel value is found under LANG/TE/TERM
     * parent.definition is  under LANG/TE/DEF
     * parent.source-elements are found under LANG/TE/SOURF, LANG/TE/DEF/SOURF and LANG/TE/NOTE/SOURF all of them are mapped to same source-list
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
     * @param o LANGType containing incoming NTRF-block
     * @param vocabularity Graph-element containing  information of  parent vocabularity like id and base-uri
     */
    private int hadleLANG(List<GenericNode> termsList, LANGType o, Map<String, List<Attribute>> parentProperties, Map<String, List<Identifier>> parentReferences, Graph vocabularity)  {
         // generate random UUID as a code and use it as part if the generated URI
         String code = UUID.randomUUID().toString();

        logger.info("Handle LANG:"+ o.getValueAttribute());

        // Attributes  are stored to property-list
        Map<String, List<Attribute>> properties = new HashMap<>();
        List<?> e=o.getTEOrDEFOrNOTE();
        // Filter TE elemets as list and add mapped elements as properties under node
        List<TEType> terms = e.stream().filter(t -> t instanceof TEType).map(t -> (TEType)t).collect(Collectors.toList());
        for(TEType i:terms){
            if(logger.isDebugEnabled())
                logger.debug("Handle Term:"+i.getTERM().getContent().toString());
            List<Serializable> li = i.getTERM().getContent();
            if(!li.isEmpty()) { // if value exist
                String value = getAttributeContent(i.getTERM().getContent());
                Attribute att = new Attribute(o.getValueAttribute(), value);
                addProperty("prefLabel", properties,  att);
            }
        };

        //DEFINITION
        List<DEFType> def = e.stream().filter(t -> t instanceof DEFType).map(t -> (DEFType)t).collect(Collectors.toList());
        // Definition is complex multi-line object which needs to be resolved
        for(DEFType d:def){
            handleDEF(d, o.getValueAttribute(), parentProperties, properties,vocabularity);
        }

        // NOTE
        List<NOTEType> notes = e.stream().filter(t -> t instanceof NOTEType).map(t -> (NOTEType)t).collect(Collectors.toList());
        for(NOTEType n:notes){
            handleNOTE(n, o.getValueAttribute(), parentProperties, properties, vocabularity);
        }

        // SOURF
        List<DEFType.SOURF> sources = e.stream().filter(t -> t instanceof DEFType.SOURF).map(t -> (DEFType.SOURF)t).collect(Collectors.toList());
        for(DEFType.SOURF s:sources){
            handleSOURF(s, o.getValueAttribute(), parentProperties,vocabularity);
        }

        // SY (synonym)
        List<SYType> synonym = e.stream().filter(t -> t instanceof SYType).map(t -> (SYType)t).collect(Collectors.toList());
        for(SYType s:synonym) {
            GenericNode n = handleSY(s, o.getValueAttribute(), parentProperties, parentReferences, vocabularity);
            if(n != null){
                termsList.add(n);

                List<Identifier> ref;
                if(parentReferences.get("altLabelXl") != null)
                    ref = parentReferences.get("altLabelXl");
                else
                    ref = new ArrayList<>();
                ref.add(new Identifier(n.getId(), typeMap.get("Term").getDomain()));
                parentReferences.put("altLabelXl",ref);
            }
        }

        TypeId typeId = typeMap.get("Term").getDomain();
        // Uri is  parent-uri/term-'code'
        GenericNode node = null;
        if(idMap.get(code) != null) {
            if(logger.isDebugEnabled())
                logger.debug("Update Term");
            node = new GenericNode(idMap.get(code),code, vocabularity.getUri() + "term-" + code, 0L, "", new Date(), "", new Date(), typeId, properties, emptyMap(), emptyMap());
        }
        else {
            node = new GenericNode(code, vocabularity.getUri() + "term-" + code, 0L, "", new Date(), "", new Date(), typeId, properties, emptyMap(), emptyMap());
            // Set just created term as preferred term for concept

            List<Identifier> ref;
            if(parentReferences.get("prefLabelXl") != null)
                ref = parentReferences.get("prefLabelXl");
            else
                ref = new ArrayList<>();
            ref.add(new Identifier(node.getId(),typeId));
            parentReferences.put("prefLabelXl",ref);
        }
        termsList.add(node);
        // Add id for reference resolving
        createdIdMap.put(node.getCode(),node.getId());
        return termsList.size();
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
    private Attribute handleCHECK(String o, String stat, Map<String, List<Attribute>> properties){
        System.out.println(" Set status: " + o);
        String status = "DRAFT";
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
            status = "VALID";
        if(stat != null && !stat.isEmpty() && stat.equalsIgnoreCase("vanhentunut"))
            status="RETIRED";
        // @TODO! Handle rest of the states
        Attribute att = new Attribute("", status);
        addProperty("status", properties, att);
        return att;
    }

    private void handleStat(String stat, Map<String, List<Attribute>> properties){
        String status = "DRAFT";
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
        if(stat != null && !stat.isEmpty() && stat.equalsIgnoreCase("vanhentunut")) {
            status = "RETIRED";
            Attribute att = new Attribute("", status);
            addProperty("status", properties, att);
        }
        return;
    }

    /**
     * NTRF Broader-concept parsing
     * Can be direct hierarchical or  partitive reference
     *    <BCON href="#tmpOKSAID122" typr="generic">koulutus (2)</BCON>
     *    <BCON href="#tmpOKSAID148" typr="partitive">toimipisteen</BCON>
     * @param o
     * @param references
     */
    private void handleBCON(BCONType o, Map<String, List<Identifier>>references) {
        System.out.println("--BCON=" + o.getHref());
        String brefId = o.getHref();
        // Remove #
        if (brefId.startsWith("#"))
             brefId = o.getHref().substring(1);

        UUID refId = idMap.get(brefId);
        if (refId == null)
            refId = createdIdMap.get(brefId);

        // partitive = isPartOf = koostumussuhteinen yläkäsite
        // generic = broader = hierarkkinen yläkäsite
        List<Identifier> ref = null;
        if(o.getTypr().equalsIgnoreCase("generic")) {
            ref = references.get("broader");
        } else
            ref = references.get("isPartOf");
        if (ref == null)
            ref = new ArrayList<>();
        // Generic = broader concept, partitive = related concept
        if (refId != null) {
            ref.add(new Identifier(refId, typeMap.get("Concept").getDomain()));
            if(o.getTypr().equalsIgnoreCase("generic")) {
                references.put("broader", ref);
            } else
                references.put("isPartOf", ref);
        }
        else
            logger.warn("BCON reference match failed. for "+brefId);
    }

    private void handleNCON(NCONType o, UUID broaderConceptId) {
        Map<String, List<Identifier>>references;
        System.out.println("--NCON=" + o.getHref());
        String nrefId = o.getHref();
        // Remove #
        if (nrefId.startsWith("#"))
            nrefId = o.getHref().substring(1);

        UUID refId = idMap.get(nrefId);
        if (refId == null)
            refId = createdIdMap.get(nrefId);

        System.out.println("Search Match:" + idMap.get(nrefId));
        System.out.println("Search Match new:" + this.createdIdMap.get(nrefId));
        if(refId == null){
            // Add placeholder for and resolve it after all items are created
            System.out.println("Can't resolve NCON-reference ID for "+nrefId);
            NconRef nconRef = new NconRef();
            nconRef.setReferenceString(nrefId);
            // Null id, as a placeholder
            nconRef.setId(NULL_ID);
            nconRef.setType(o.getTypr());
            nconRef.setTargetId(broaderConceptId);
            nconList.add(nconRef);
        } else {
            NconRef nconRef = new NconRef();
            nconRef.setReferenceString(nrefId);
            nconRef.setId(refId);
            nconRef.setType(o.getTypr());
            nconRef.setTargetId(broaderConceptId);
            nconList.add(nconRef);
        }
    }

    private Attribute handleCLAS(String o, Map<String, List<Attribute>> properties){
        System.out.println(" Set clas: " + o);
        Attribute att = new Attribute("", o);
        addProperty("conceptClass", properties, att);
        return att;
    }

    private Attribute  handleDEF( DEFType def, String lang, Map<String, List<Attribute>>  parentProperties, Map<String, List<Attribute>>  termProperties,  Graph vocabularity){
        if(logger.isDebugEnabled())
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
                        if(rc.getTypr() != null && !rc.getTypr().isEmpty()) {
                            defString = defString.concat(" data-typr ='" +
                                    rc.getTypr()+"'");
                        }
                        defString = defString.concat(">"+rc.getContent().get(0)+ "</a>");
                    } else if(j.getName().toString().equalsIgnoreCase("BCON")){
                        //<DEF><RCON href="#tmpOKSAID162">yliopiston</RCON> <BCON href="#tmpOKSAID187" typr="partitive"
                        // >opetus- ja tutkimushenkilöstön</BCON> osa, jonka tehtävissä suunnitellaan,
                        // koordinoidaan ja johdetaan erittäin laajoja kokonaisuuksia, tehtäviin sisältyy kokonaisvaltaista
                        // vastuuta organisaation toiminnasta ja taloudesta sekä kansallisen tai kansainvälisen
                        // tason kehittämistehtävistä ja tehtävissä vaikutetaan huomattavasti koko tutkimusjärjestelmään
                        // <SOURF>neliport + tr40</SOURF></DEF>

                        BCONType bc=(BCONType)j.getValue();
                        defString = defString.concat("<a href='"+
                                vocabularity.getUri());
                        // Remove # from uri
                        if(bc.getHref().startsWith("#")) {
                            defString = defString.concat(bc.getHref().substring(1) + "'");
                        } else
                            defString = defString.concat(bc.getHref() + "'");
                        if(bc.getTypr() != null && !bc.getTypr().isEmpty()) {
                            defString = defString.concat(" data-typr ='" +
                                    bc.getTypr()+"'");
                        }
                        defString = defString.concat(">"+bc.getContent().get(0)+ "</a>");
                    } else if(j.getName().toString().equalsIgnoreCase("SOURF")) {
                        DEFType.SOURF sf = (DEFType.SOURF)j.getValue();
                        if(sf.getContent()!= null && sf.getContent().size() >0) {
                            // Add  refs as sources-part.
                            updateSources(sf.getContent(), lang, termProperties);
                        }
                    }
                }
            }
        };
        if(logger.isDebugEnabled())
            logger.debug("Definition="+defString);
        // Add definition if exist.
        if(!defString.isEmpty()) {
            Attribute att = new Attribute(lang, defString);
            addProperty("definition", parentProperties, att);
            return att;
        } else
            return null;
    }

    private Attribute  handleNOTE( NOTEType note, String lang, Map<String, List<Attribute>>  parentProperties,Map<String, List<Attribute>>  termProperties, Graph vocabularity){
        if(logger.isDebugEnabled())
            logger.debug("handleNOTE-part"+note.getContent());

        String noteString="";

        List<Serializable> noteItems = note.getContent();
        for(Serializable de:noteItems) {
            if(de instanceof  String) {
                if(logger.isDebugEnabled())
                    logger.debug("  Parsing note-string:" + de.toString());
                noteString =noteString.concat(de.toString());
            }
            else {
                if(de instanceof JAXBElement){
                    JAXBElement j = (JAXBElement)de;
                    if(logger.isDebugEnabled())
                        logger.debug("  Parsing note-elem:" + j.getName()+"");
                    if(j.getName().toString().equalsIgnoreCase("RCON")){
                        RCONType rc=(RCONType)j.getValue();
                        noteString = noteString.concat("<a href='"+
                                vocabularity.getUri());
                        // Remove # from uri
                        if(rc.getHref().startsWith("#")) {
                            noteString = noteString.concat(rc.getHref().substring(1) + "'");
                        } else
                            noteString = noteString.concat(rc.getHref() + "'");
                        if(rc.getTypr() != null && !rc.getTypr().isEmpty()) {
                            noteString = noteString.concat(" data-typr ='" +
                                    rc.getTypr()+"'");
                        }
                        noteString = noteString.concat(">"+rc.getContent().get(0)+ "</a>");
                    }
                    else if(j.getName().toString().equalsIgnoreCase("SOURF")) {
                        NOTEType.SOURF sf = (NOTEType.SOURF)j.getValue();
                        if(sf.getContent()!= null && sf.getContent().size() >0) {
                            noteString.concat(sf.getContent().toString());
                            // Add  refs as string and  construct lines four sources-part.
                            updateSources(sf.getContent(), lang, termProperties);
                        }
                    } else if(j.getName().toString().equalsIgnoreCase("LINK")){
                        // External link
                        LINKType l=(LINKType)j.getValue();
                        // Remove  "href:" from string "href:https://www.finlex.fi/fi/laki/ajantasa/1973/19730036"
                        String url=l.getHref().substring(5);
                        noteString = noteString.concat("<a href='"+url+"' data-type='external'>"+l.getContent().get(0)+"</a>");
                    } else
                        System.out.println("  note-class" + de.getClass().getName());
                }
                if(logger.isDebugEnabled())
                    logger.debug("note-String="+noteString);
            }
        };

        // Add note if exist.
        if(!noteString.isEmpty()) {
            Attribute att = new Attribute(lang, noteString);
            addProperty("note", parentProperties, att);
            return att;
        } else
            return null;
    }
    /**
     * Sample of incoming synonyms
     *       <SY>
     *         <TERM>examensarbete<GRAM gend="n"></GRAM></TERM>
     *         <SCOPE>akademisk</SCOPE>
     *         <SOURF>fisv_utbild_ordlista + kielityoryhma_sv</SOURF>
     *       </SY>
     *       <SY>
     *         <EQUI value="near-equivalent"></EQUI>
     *         <TERM>vetenskapligt arbete<GRAM gend="n"></GRAM></TERM>
     *         <SCOPE>akademisk</SCOPE>
     *         <SOURF>fisv_utbild_ordlista + kielityoryhma_sv</SOURF>
     *       </SY>
     */

    private GenericNode handleSY(SYType synonym, String lang, Map<String, List<Attribute>>  parentProperties, Map<String, List<Identifier>> parentReferences, Graph vocabularity){
        if(logger.isDebugEnabled())
            logger.debug("handleSY-part:"+synonym.getEQUIOrTERMOrHOGR());
        //Synonym fields
        String equi= "";
        String hogr = "";
        String term ="";
        String sourf = "";
        String scope = "";
        // Attributes  are stored to property-list
        Map<String, List<Attribute>> properties = new HashMap<>();

        for(JAXBElement elem:synonym.getEQUIOrTERMOrHOGR()) {
            if(logger.isDebugEnabled())
                logger.debug("Parsing SYN-Element=" + elem.getName().toString());
            if (elem.getName().toString().equalsIgnoreCase("EQUI")) {
                // Attribute string value = broader | narrower | near-equivalent
                EQUIType eqt = (EQUIType)elem.getValue();
                equi = eqt.getValueAttribute();
                String eqvalue ="=";
                if(equi.equalsIgnoreCase("broader"))
                    eqvalue=">";
                if(equi.equalsIgnoreCase("narrower"))
                    eqvalue="<";
                if(equi.equalsIgnoreCase("near-equivalent"))
                    eqvalue="~";

                Attribute att = new Attribute(lang, eqvalue);
                addProperty("termEquivalency", properties,  att);
            }else if (elem.getName().toString().equalsIgnoreCase("HOGR")) {
                hogr= elem.getValue().toString();
                Attribute att = new Attribute(lang, hogr);
                addProperty("termHomographNumber", properties,  att);
            } else if (elem.getName().toString().equalsIgnoreCase("TERM")) {
                System.out.println(elem.getValue());
                if(elem.getValue() instanceof SYType.TERM){
                    SYType.TERM termObj = (SYType.TERM)elem.getValue();
                    term = termObj.getContent().toString();
                    List<Serializable> termValues = termObj.getContent();
                    for(Serializable t:termValues){
                        // If is name and it may contain additional GRAM-specification
                        if(t instanceof  String) {
                            term = t.toString();
                        } else{
                            // Term can contain parts like GRAM-elements
                            if(t instanceof JAXBElement){
                                JAXBElement el = (JAXBElement)t;
                                System.out.println("    Term val=" + el.getName().toString());

                                if(el.getName().toString().equals("GRAM")){
                                    GRAMType gt = (GRAMType) el.getValue();
                                    System.out.println("Grammatical specification: gender    ="+gt.getGend());
                                    System.out.println("               verb/noun   pos       ="+gt.getPos());
                                    System.out.println("                           value     ="+gt.getValue());
                                    System.out.println("                yks/mon    value Att ="+gt.getValueAttribute());
                                    // termConjugation (single, plural)
                                    if(gt.getValueAttribute() != null && gt.getValueAttribute().equalsIgnoreCase("pl")){
                                        // Currently not localized
                                        Attribute att = new Attribute("fi", "monikko");
                                        addProperty("termConjugation", properties,  att);
                                    } else if(gt.getValueAttribute() != null && gt.getValueAttribute().equalsIgnoreCase("n pl")){
                                        // Currently not localized plural and  neutral
                                        Attribute att = new Attribute("fi", "monikko");
                                        addProperty("termConjugation", properties,  att);
                                        att = new Attribute("fi", "neutri");
                                        addProperty("termFamily", properties,  att);
                                    }else if(gt.getValueAttribute() != null && gt.getValueAttribute().equalsIgnoreCase("f pl")){
                                        // Currently not localized plural and  neutral
                                        Attribute att = new Attribute("fi", "monikko");
                                        addProperty("termConjugation", properties,  att);
                                        att = new Attribute("fi", "feminiini");
                                        addProperty("termFamily", properties,  att);
                                    }
                                    // termFamily
                                    if(gt.getGend() != null && gt.getGend().equalsIgnoreCase("f")){
                                        // feminiini
                                        // Currently not localized
                                        Attribute att = new Attribute("fi", "feminiini");
                                        addProperty("termFamily", properties,  att);
                                    } else if(gt.getGend() != null && gt.getGend() != null && gt.getGend().equalsIgnoreCase("m")){
                                        // maskuliiini
                                        Attribute att = new Attribute("fi", "maskuliini");
                                        addProperty("termFamily", properties,  att);
                                    } else if(gt.getGend() != null && gt.getGend().equalsIgnoreCase("n")){
                                        // Neutri
                                        Attribute att = new Attribute("fi", "neutri");
                                        addProperty("termFamily", properties,  att);
                                    }
                                    // wordClass
                                    if(gt.getPos() != null && !gt.getPos().isEmpty()){
                                        // Currently not localized, just copy wordClass as such
                                        Attribute att = new Attribute("fi", gt.getPos());
                                        addProperty("wordClass", properties,  att);
                                    }
                                }
                            }
                        }
                    }
                    Attribute att = new Attribute(lang, term);
                    addProperty("prefLabel", properties,  att);
                }
            } else if (elem.getName().toString().equalsIgnoreCase("SOURF")) {
                // Here it is just string
                sourf=elem.getValue().toString();

                Attribute att = new Attribute(lang, sourf);
                addProperty("source", properties,  att);

                if(sourf!= null && !sourf.isEmpty()) {
                    // Add  refs as string and  construct lines four sources-part.
                    updateSources(sourf, lang, properties);
                }
            } else if (elem.getName().toString().equalsIgnoreCase("SCOPE")) {
                scope = elem.getValue().toString();
                Attribute att = new Attribute(lang, scope);
                addProperty("scope", properties,  att);
            }
        }
        // create new synonyme node (Term)
        TypeId typeId = typeMap.get("Term").getDomain();
        // Uri is  parent-uri/term-'code'
        GenericNode node = null;
        UUID id = UUID.randomUUID();
        String code = id.toString();

        node = new GenericNode(id, code, vocabularity.getUri() + "term-" + code, 0L, "", new Date(), "", new Date(), typeId, properties, emptyMap(), emptyMap());
        // Add id for reference resolving
        createdIdMap.put(node.getCode(),node.getId());
        return node;
    }

    /**
     *  From
     * @param source
     * @param lang
     * @param properties
     * @param vocabularity
     * @return
     */
    private Attribute  handleSOURF(DEFType.SOURF source, String lang, Map<String, List<Attribute>>  properties, Graph vocabularity){
        if(logger.isDebugEnabled())
            logger.debug("handleSOURF-part"+source.getContent());

        String sourceString="";

        List<Serializable> sourceItems = source.getContent();
        for(Serializable se:sourceItems) {
            if(se instanceof  String) {
                System.out.println("  SOURF-string"+se.toString());
                sourceString =sourceString.concat(se.toString());
            }
            else {
                if(se instanceof JAXBElement){
                    JAXBElement j = (JAXBElement)se;
                    System.out.println("  SOURF-elem <" + j.getName()+">");
                    if(j.getName().toString().equalsIgnoreCase("SOURF")){
                        RCONType rc=(RCONType)j.getValue();
                        sourceString = sourceString.concat("<a href='"+
                                vocabularity.getUri());
                        if(rc.getTypr() != null && !rc.getTypr().isEmpty()) {
                            sourceString = sourceString.concat(" data-typr ='" +
                                    rc.getTypr()+"'");
                        }
                        sourceString = sourceString.concat(">"+rc.getContent().get(0)+ "</a>");
                    }
                    else if(j.getName().toString().equalsIgnoreCase("SOURF")) {
                        NOTEType.SOURF sf = (NOTEType.SOURF)j.getValue();
                        if(sf.getContent()!= null && sf.getContent().size() >0) {
                            sourceString = sourceString.concat(" "+sf.getContent());
                            // Add  refs as string and  construct lines four sources-part.
                            updateSources(sf.getContent(), lang, properties);
                        }
                    }  else
                        System.out.println("  SOURF-class" + se.getClass().getName());
                }
                System.out.println("  -----SOURF-String=|" + sourceString+"|");
            }
        };
        // Add definition if exist.

        if(!sourceString.isEmpty()) {
            Attribute att = new Attribute(lang, sourceString);
            addProperty("source", properties, att);
            return att;
        } else
            return null;
    }

    /**
     * Add individual source-elements to the source-list for each individual reference  enumerated inside imported SOURF
     * @param srefs
     * @param lang
     * @param properties
     */
    private void updateSources(List<Serializable> srefs, String lang,  Map<String, List<Attribute>> properties){
        for(Serializable o:srefs) {
            updateSources(o.toString(),lang, properties);
        };
    }

    /**
     * Add individual source-elements sfrom give string
     * @param srefs
     * @param lang
     * @param properties
     */
    private void updateSources(String srefs, String lang,  Map<String, List<Attribute>> properties){
            String fields[] = srefs.split("\\+");
            for (String s : fields) {
                s = s.trim();
                String sourcesString="["+s+"]";
                Map<String, String> m = referenceMap.get(s);
                if (m != null) {
                    if (m.get("text") != null && !m.get("text").isEmpty()) {
                        sourcesString = sourcesString.concat("\n "+m.get("text")+"\n");
                    }
                    if (m.get("url") != null && !m.get("url").isEmpty()) {
                        sourcesString = sourcesString.concat( m.get("url"));
                    }
                } else {
                    logger.warn("Not matching reference found for:"+s);
                }
                if(!sourcesString.isEmpty()) {
                    if(logger.isDebugEnabled())
                        logger.debug("ADDING sourf:"+sourcesString);
                    Attribute satt = new Attribute(lang, sourcesString);
                    addProperty("source", properties, satt);
                }
            }
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

    private class NconRef {
        String referenceString;
        String type;
        UUID id;
        UUID targetId;

        public String getReferenceString() {
            return referenceString;
        }

        public void setReferenceString(String referenceString) {
            this.referenceString = referenceString;
        }

        public UUID getTargetId() {
            return targetId;
        }

        public void setTargetId(UUID targetId) {
            this.targetId = targetId;
        }

        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }
}
