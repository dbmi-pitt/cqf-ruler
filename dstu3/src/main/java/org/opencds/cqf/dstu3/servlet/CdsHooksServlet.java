package org.opencds.cqf.dstu3.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.*;

import org.apache.http.entity.ContentType;
import org.cqframework.cql.elm.execution.Library;
import org.hl7.fhir.dstu3.model.*;
import org.opencds.cqf.cds.discovery.DiscoveryResolutionStu3;
import org.opencds.cqf.cds.evaluation.EvaluationContext;
import org.opencds.cqf.cds.evaluation.Stu3EvaluationContext;
import org.opencds.cqf.cds.exceptions.MissingRequiredFieldException;
import org.opencds.cqf.cds.hooks.Hook;
import org.opencds.cqf.cds.hooks.HookFactory;
import org.opencds.cqf.cds.hooks.Stu3HookEvaluator;
import org.opencds.cqf.cds.providers.ProviderConfiguration;
import org.opencds.cqf.cds.request.JsonHelper;
import org.opencds.cqf.cds.request.Request;
import org.opencds.cqf.cds.response.CdsCard;
import org.opencds.cqf.common.config.HapiProperties;
import org.opencds.cqf.common.exceptions.InvalidRequestException;
import org.opencds.cqf.common.providers.LibraryResolutionProvider;
import org.opencds.cqf.common.retrieve.JpaFhirRetrieveProvider;
import org.opencds.cqf.cql.engine.data.CompositeDataProvider;
import org.opencds.cqf.cql.engine.debug.DebugMap;
import org.opencds.cqf.cql.engine.exception.CqlException;
import org.opencds.cqf.cql.engine.execution.Context;
import org.opencds.cqf.cql.engine.execution.LibraryLoader;
import org.opencds.cqf.cql.engine.fhir.exception.DataProviderException;
import org.opencds.cqf.cql.engine.fhir.model.Dstu3FhirModelResolver;
import org.opencds.cqf.dstu3.config.CdsConfiguration;
import org.opencds.cqf.dstu3.helpers.LibraryHelper;
import org.opencds.cqf.dstu3.providers.JpaTerminologyProvider;
import org.opencds.cqf.dstu3.providers.PlanDefinitionApplyProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;

@WebServlet(name = "cds-services")
public class CdsHooksServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private FhirVersionEnum version = FhirVersionEnum.DSTU3;
    private static final Logger logger = LoggerFactory.getLogger(CdsHooksServlet.class);

    private org.opencds.cqf.dstu3.providers.PlanDefinitionApplyProvider planDefinitionProvider;

    private LibraryResolutionProvider<org.hl7.fhir.dstu3.model.Library> libraryResolutionProvider;

    private JpaFhirRetrieveProvider fhirRetrieveProvider;

    private org.opencds.cqf.dstu3.providers.JpaTerminologyProvider jpaTerminologyProvider;

    private ProviderConfiguration providerConfiguration;

    private CdsConfiguration config;

    private Reference user;


    @SuppressWarnings("unchecked")
    @Override
    public void init() {
        // System level providers
        ApplicationContext appCtx = (ApplicationContext) getServletContext()
                .getAttribute("org.springframework.web.context.WebApplicationContext.ROOT");

        this.providerConfiguration = appCtx.getBean(ProviderConfiguration.class);
        this.planDefinitionProvider = appCtx.getBean(PlanDefinitionApplyProvider.class);
        this.libraryResolutionProvider = (LibraryResolutionProvider<org.hl7.fhir.dstu3.model.Library>) appCtx.getBean(LibraryResolutionProvider.class);
        this.fhirRetrieveProvider = appCtx.getBean(JpaFhirRetrieveProvider.class);
        this.jpaTerminologyProvider = appCtx.getBean(JpaTerminologyProvider.class);
    }

    protected ProviderConfiguration getProviderConfiguration() {
        return this.providerConfiguration;
    }

    // CORS Pre-flight
    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setAccessControlHeaders(resp);

        resp.setHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType());
        resp.setHeader("X-Content-Type-Options", "nosniff");

        resp.setStatus(HttpServletResponse.SC_OK);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        logger.info(request.getRequestURI());
        if (!request.getRequestURL().toString().endsWith("cds-services")) {
            logger.error(request.getRequestURI());
            throw new ServletException("This servlet is not configured to handle GET requests.");
        }

        this.setAccessControlHeaders(response);
        response.setHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType());
        response.getWriter().println(new GsonBuilder().setPrettyPrinting().create().toJson(getServices()));
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        logger.info(request.getRequestURI());

        try {
            // validate that we are dealing with JSON
            if (request.getContentType() == null || !request.getContentType().startsWith("application/json")) {
                throw new ServletException(String.format("Invalid content type %s. Please use application/json.", request.getContentType()));
            }


            String baseUrl = HapiProperties.getServerAddress();
            String service = request.getPathInfo().replace("/", "");

            JsonParser parser = new JsonParser();
            JsonObject requestJson = parser.parse(request.getReader()).getAsJsonObject();
            logger.info(requestJson.toString());

            Request cdsHooksRequest = new Request(service, requestJson, JsonHelper.getObjectRequired(getService(service), "prefetch"));
            try {
                user = new Reference(JsonHelper.getStringRequired(requestJson, "userId"));
            } catch (MissingRequiredFieldException missingRequiredFieldException) {
                try {
                    user = new Reference(cdsHooksRequest.getContext().getContextJson().get("userId").getAsString());
                } catch (Exception exception) {
                    throw missingRequiredFieldException;
                }
            }

            JsonObject extJsonObj = JsonHelper.getObjectOptional(requestJson, "extension");
            if (extJsonObj != null) {
                JsonObject configJsonObj = (JsonObject) extJsonObj.get("pddi-configuration-items");
                if (configJsonObj != null) {
                    Boolean showEvSupport = null;
                    if (configJsonObj.get("show-evidence-support") != null)
                        showEvSupport = configJsonObj.get("show-evidence-support").getAsBoolean();

                    Boolean alertNonSerious = null;
                    if (configJsonObj.get("alert-non-serious") != null)
                        alertNonSerious = configJsonObj.get("alert-non-serious").getAsBoolean();

                    Boolean cacheForOrderSignFiltering = null;
                    if (configJsonObj.get("cache-for-order-sign-filtering") != null)
                        cacheForOrderSignFiltering = configJsonObj.get("cache-for-order-sign-filtering").getAsBoolean();

                    Boolean filterOutRepeatedAlerts = null;
                    if (configJsonObj.get("filter-out-repeated-alerts") != null)
                        filterOutRepeatedAlerts = configJsonObj.get("filter-out-repeated-alerts").getAsBoolean();

                    if (showEvSupport != null && alertNonSerious != null && (cacheForOrderSignFiltering != null || filterOutRepeatedAlerts != null)) {
                        System.out.println("DEBUG: CdsRequest::CdsRequest - pddi-configuration-items found in the extension object and validated. showEvSupport = " +
                                showEvSupport + ", alertNonSerious = " + alertNonSerious + ", cacheForOrderSignFiltering = " + cacheForOrderSignFiltering +
                                ", filterOutRepeatedAlerts = " + filterOutRepeatedAlerts);
                        this.config = new CdsConfiguration(configJsonObj, alertNonSerious, showEvSupport, cacheForOrderSignFiltering, filterOutRepeatedAlerts);

                    } else {
                        throw new RuntimeException("ERROR: CdsRequest::CdsRequest - pddi-configuration-items found in the extension object of the request but the required properties failed validation. Be sure that show-evidence-support and alert-non-serious both present and both boolean and that either cacheForOrderSignFiltering or filterOutRepeatedAlerts are also present and boolean;");
                    }
                }
            }


            Hook hook = HookFactory.createHook(cdsHooksRequest);

            logger.info("cds-hooks hook: " + hook.getRequest().getHook());
            logger.info("cds-hooks hook instance: " + hook.getRequest().getHookInstance());
            logger.info("cds-hooks maxCodesPerQuery: " + this.getProviderConfiguration().getMaxCodesPerQuery());
            logger.info("cds-hooks expandValueSets: " + this.getProviderConfiguration().getExpandValueSets());
            logger.info("cds-hooks searchStyle: " + this.getProviderConfiguration().getSearchStyle());
            logger.info("cds-hooks prefetch maxUriLength: " + this.getProviderConfiguration().getMaxUriLength());
            logger.info("cds-hooks local server address: " + baseUrl);
            logger.info("cds-hooks fhir server address: " + hook.getRequest().getFhirServerUrl());

            PlanDefinition planDefinition = planDefinitionProvider.getDao().read(new IdType(hook.getRequest().getServiceName()));
            LibraryLoader libraryLoader = LibraryHelper.createLibraryLoader(libraryResolutionProvider);
            Library library = LibraryHelper.resolvePrimaryLibrary(planDefinition, libraryLoader, libraryResolutionProvider);

            Dstu3FhirModelResolver resolver = new Dstu3FhirModelResolver();
            CompositeDataProvider provider = new CompositeDataProvider(resolver, fhirRetrieveProvider);

            Context context = new Context(library);

            DebugMap debugMap = new DebugMap();
            debugMap.setIsLoggingEnabled(true);
            context.setDebugMap(debugMap);

            context.registerDataProvider("http://hl7.org/fhir", provider); // TODO make sure tooling handles remote
                                                                           // provider case
            context.registerTerminologyProvider(jpaTerminologyProvider);
            context.registerLibraryLoader(libraryLoader);
            context.setContextValue("Patient", hook.getRequest().getContext().getPatientId().replace("Patient/", ""));
            context.setExpressionCaching(true);

            EvaluationContext<PlanDefinition> evaluationContext = new Stu3EvaluationContext(hook, version, FhirContext.forDstu3().newRestfulGenericClient(baseUrl),
                    jpaTerminologyProvider, context, library,
                    planDefinition, this.getProviderConfiguration());

            this.setAccessControlHeaders(response);

            response.setHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType());

            Stu3HookEvaluator evaluator = new Stu3HookEvaluator();

            List<CdsCard> cdsCards = evaluator.evaluate(evaluationContext);
            if (this.config == null) {
                // return cards if there is no configuration that would alter the results
                String jsonResponse = toJsonResponse(cdsCards);
                logger.info(jsonResponse);
                response.getWriter().println(jsonResponse);
            } else {
                // filter the returning cards depending on the the configuration

                // If the configuration specifies to not show cards unless
                // they are 'serious', identify the non-serious cards and
                // index them for removal
                Integer ctr = 0;
                if (this.config.getAlertNonSerious() != null && this.config.getAlertNonSerious() == false) {
                    List<Integer> cardsToRemove = new ArrayList<Integer>();
                    ListIterator<CdsCard> litr = cdsCards.listIterator();

                    while (litr.hasNext()) {
                        CdsCard card = litr.next();
                        if (card.hasIndicator() &&
                                (card.getIndicator() == CdsCard.IndicatorCode.WARN ||
                                        card.getIndicator() == CdsCard.IndicatorCode.INFO)) {
                            System.out.println("DEBUG: CdsRequest::process - pddi-configuration-items found in the extension object. Test for non-serious cards found one. Index: " + ctr + " card.indicator: " + card.getIndicator());
                            cardsToRemove.add(ctr);
                        }
                        ctr = ctr += 1;
                    }

                    // removes non serious cards if config mandates
                    // it. These will be indexed in the cardsToRemove
                    // List. The adjustment counter is needed b/c java
                    // List::remove shifts the elements left upon each
                    // removal. Nothing will happen to the card list if
                    // there are no items in the index of cards to remove
                    ListIterator<Integer> removeLitr = cardsToRemove.listIterator();
                    Integer adjustment = 0;
                    while (removeLitr.hasNext()) {
                        System.out.println("DEBUG: CdsRequest::process - pddi-configuration- Removing non-serious card.");
                        cdsCards.remove(removeLitr.next() - adjustment);
                        adjustment = adjustment + 1;
                    }
                }


                if (this.config.getShowEvidenceSupport() != null && this.config.getShowEvidenceSupport() == false) {
                    // removes the evidence strings present in the detail
                    // attribute of the CdsCard if the config mandates it.
                    ListIterator<CdsCard> litr = cdsCards.listIterator();
                    while (litr.hasNext()) {
                        System.out.println("DEBUG: CdsRequest::process - pddi-configuration- Replacing card detail with an empty string");
                        litr.next().setDetail("");
                    }
                }


                if (this.config.getCacheForOrderSignFiltering() != null && this.config.getCacheForOrderSignFiltering() == true) {
                    // store the related artifact information from the
                    // plan definition along with the CDS Hook request
                    // user and patient for reference in future CDS Hooks
                    // requests
                    String orderingPhys = this.user.getReference();
                    String patient = hook.getRequest().getContext().getPatientId();
                    String encounter = hook.getRequest().getContext().getEncounterId(); // TODO: note in the IG that encounter ids are  REQUIRED in PDDI cds hooks requests
                    // TODO: we might want to use the Medication resource
                    // pointed to by 'selections' attribute of the order
                    // select to add more specific data for filtering here

                    // find the documentation related artifact and obtain
                    // the URL which serves as an identifier to the
                    // knowledge artifact of interest

                    String planDefinitionUrl = planDefinition.getUrl();
                    String knowledgeArtifactUrl = hook.getRequest().getFhirServerUrl() + planDefinitionUrl.substring(planDefinitionUrl.indexOf("/Plan"));

                    System.out.println("DEBUG: knowledgeArtifactUrl: " + knowledgeArtifactUrl);


                    // medicationObject will contain the base 64 encoded version of the entry grabbed from order-select
                    // medicationId will be the system + code grabbed from the medicationCodeableConcept and will be used to query the DB in order-sign
                    String medicationObject = null;
                    String medicationId = null;

                    JsonArray contextSelections = hook.getRequest().getContext().getContextJson().get("selections").getAsJsonArray();
                    Map<String, String> selections = new HashMap<>();
                    for (JsonElement jsonSelection : contextSelections) {
                        String[] selection = jsonSelection.getAsString().split("/");
                        // Selections map will be ID to resource type
                        selections.put(selection[1], selection[0]);
                    }
                    JsonObject draftOrders = JsonHelper.getObjectRequired(hook.getRequest().getContext().getContextJson(), "draftOrders");
                    JsonArray orderEntries = draftOrders.get("entry").getAsJsonArray();

                    CdsHooksPersistOrderSelect dbCon = new CdsHooksPersistOrderSelect();
                    Boolean persistFlag = false;

                    // For each entry in "draftOrders" we check if the resource type and the id equals the selection then save that entry
                    for (JsonElement entry : orderEntries) {
                        JsonObject members = JsonHelper.getObjectRequired((JsonObject) entry, "resource");
                        if (members.has("id") && members.has("resourceType")) {
                            String checkId = JsonHelper.getStringRequired(members, "id");
                            String checkResourceType = JsonHelper.getStringRequired(members, "resourceType");
                            if (selections.containsKey(checkId) && selections.get(checkId).equals(checkResourceType)) {
                                medicationObject = members.toString();

                                JsonObject codeableConcept = JsonHelper.getObjectRequired(members, "medicationCodeableConcept");
                                JsonObject coding = codeableConcept.get("coding").getAsJsonArray().get(0).getAsJsonObject();
                                medicationId = JsonHelper.getStringRequired(coding, "system") + JsonHelper.getStringRequired(coding, "code");
                                System.out.println("DEBUG: orderingPhys: " + orderingPhys + ", patient: " + patient + ", encounter:" + encounter + ", medicationId: " + medicationId + ", medicationObject: " + medicationObject);
                                persistFlag = dbCon.persistOrderSelectRequestData(orderingPhys, patient, encounter, medicationId, medicationObject, knowledgeArtifactUrl, cdsCards);

                            }
                        }
                    }

                    if (persistFlag == true)
                        System.out.println("DEBUG: data persisted to database for use during order sign card filtering.");
                    else
                        System.out.println("DEBUG: data was NOT persisted to database due to an error. Please see the logs.");

                }


                if (this.config.getFilterOutRepeatedAlerts() != null && this.config.getFilterOutRepeatedAlerts() == true) {
                    // check the data persisted from order select to see
                    // if the request involves the same plan definition
                    // knowledge artifact, user, patient, and
                    // encounter. If so, return empty cards.
                    String orderingPhys = this.user.getReference();
                    String patient = hook.getRequest().getContext().getPatientId();
                    String encounter = hook.getRequest().getContext().getEncounterId(); // TODO: note in the IG that encounter ids are  REQUIRED in PDDI cds hooks requests

                    // find the documentation related artifact and obtain
                    // the URL which serves as an identifier to the
                    // knowledge artifact of interest

                    String planDefinitionUrl = planDefinition.getUrl();
                    String knowledgeArtifactUrl = hook.getRequest().getFhirServerUrl() + planDefinitionUrl.substring(planDefinitionUrl.indexOf("/Plan")).replace("sign", "select");

                    System.out.println("DEBUG: knowledgeArtifactUrl: " + knowledgeArtifactUrl);

                    int cdsCardLength = cdsCards.size();
                    CdsHooksPersistOrderSelect dbCon = new CdsHooksPersistOrderSelect();

                    String medicationId = null;
                    JsonObject draftOrders = JsonHelper.getObjectRequired(hook.getRequest().getContext().getContextJson(), "orders");
//                    JsonObject draftOrders = JsonHelper.getObjectRequired( hook.getRequest().getContext().getContextJson(), "draftOrders");
                    JsonArray orderEntries = draftOrders.get("entry").getAsJsonArray();
                    for (JsonElement entry : orderEntries) {
                        JsonObject members = JsonHelper.getObjectRequired((JsonObject) entry, "resource");
                        if (members.has("medicationCodeableConcept")) {
                            JsonObject codeableConcept = JsonHelper.getObjectRequired(members, "medicationCodeableConcept");
                            JsonObject coding = codeableConcept.get("coding").getAsJsonArray().get(0).getAsJsonObject();
                            medicationId = JsonHelper.getStringRequired(coding, "system") + JsonHelper.getStringRequired(coding, "code");
                            System.out.println("DEBUG: orderingPhys: " + orderingPhys + ", patient: " + patient + ", encounter:" + encounter + ", medication: " + medicationId);

                            //We check if this card was already shown, if so then we remove it from cdsCards
                            int priamryKey = dbCon.testForOrderSelectRequestData(orderingPhys, patient, encounter, medicationId, knowledgeArtifactUrl);
                            cdsCards = dbCon.updateCdsCards(priamryKey, cdsCards);
                        }
                    }


                    if (cdsCards.size() < cdsCardLength) {
                        System.out.println("DEBUG: test for prior record of knowledge artifact in database succeeded - filtering all cards triggered from this CDS request.");
                        CdsCard filteredCard = new CdsCard("An alert was filtered because this request is configured to filter alerts if they were presented previously in response to a prior CDS Hook request.", "info", new CdsCard.Source());
                        filteredCard.setDetail("Since filter-out-repeated-alerts was set to true in this CDS Hook request, the service is filtering out cards that were triggered by the same knowledge artifact when the physician reference display, encounter id, and patient id match between the order-select and order-sign requests.");
                        // TODO: add more details on how the filtering happens

                        cdsCards.add(filteredCard);
                    }
                }

                String jsonResponse = toJsonResponse(cdsCards);
                logger.info(jsonResponse);
                response.getWriter().println(jsonResponse);
            }


//
//            logger.info(jsonResponse);
//
//            response.getWriter().println(jsonResponse);
        } catch (BaseServerResponseException e) {
            this.setAccessControlHeaders(response);
            response.setStatus(500); // This will be overwritten with the correct status code downstream if needed.
            response.getWriter().println("ERROR: Exception connecting to remote server.");
            this.printMessageAndCause(e, response);
            this.handleServerResponseException(e, response);
            this.printStackTrack(e, response);
            logger.error(e.toString());
        } catch (DataProviderException e) {
            this.setAccessControlHeaders(response);
            response.setStatus(500); // This will be overwritten with the correct status code downstream if needed.
            response.getWriter().println("ERROR: Exception in DataProvider.");
            this.printMessageAndCause(e, response);
            if (e.getCause() != null && (e.getCause() instanceof BaseServerResponseException)) {
                this.handleServerResponseException((BaseServerResponseException) e.getCause(), response);
            }

            this.printStackTrack(e, response);
            logger.error(e.toString());
        } catch (CqlException e) {
            this.setAccessControlHeaders(response);
            response.setStatus(500); // This will be overwritten with the correct status code downstream if needed.
            response.getWriter().println("ERROR: Exception in CQL Execution.");
            this.printMessageAndCause(e, response);
            if (e.getCause() != null && (e.getCause() instanceof BaseServerResponseException)) {
                this.handleServerResponseException((BaseServerResponseException) e.getCause(), response);
            }

            this.printStackTrack(e, response);
            logger.error(e.toString());
        } catch (Exception e) {
            logger.error(e.toString());
            throw new ServletException("ERROR: Exception in cds-hooks processing.", e);
        }
    }

    private void handleServerResponseException(BaseServerResponseException e, HttpServletResponse response)
            throws IOException {
        switch (e.getStatusCode()) {
            case 401:
            case 403:
                response.getWriter().println("Precondition Failed. Remote FHIR server returned: " + e.getStatusCode());
                response.getWriter().println("Ensure that the fhirAuthorization token is set or that the remote server allows unauthenticated access.");
                response.setStatus(412);
                break;
            case 404:
                response.getWriter().println("Precondition Failed. Remote FHIR server returned: " + e.getStatusCode());
                response.getWriter().println("Ensure the resource exists on the remote server.");
                response.setStatus(412);
                break;
            default:
                response.getWriter().println("Unhandled Error in Remote FHIR server: " + e.getStatusCode());
        }
    }

    private void printMessageAndCause(Exception e, HttpServletResponse response) throws IOException {
        if (e.getMessage() != null) {
            response.getWriter().println(e.getMessage());
        }

        if (e.getCause() != null && e.getCause().getMessage() != null) {
            response.getWriter().println(e.getCause().getMessage());
        }
    }

    private void printStackTrack(Exception e, HttpServletResponse response) throws IOException {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        String exceptionAsString = sw.toString();
        response.getWriter().println(exceptionAsString);
    }

    private JsonObject getService(String service) {
        JsonArray services = getServices().get("services").getAsJsonArray();
        List<String> ids = new ArrayList<>();
        for (JsonElement element : services) {
            if (element.isJsonObject() && element.getAsJsonObject().has("id")) {
                ids.add(element.getAsJsonObject().get("id").getAsString());
                if (element.isJsonObject() && element.getAsJsonObject().get("id").getAsString().equals(service)) {
                    return element.getAsJsonObject();
                }
            }
        }
        throw new InvalidRequestException(
                "Cannot resolve service: " + service + "\nAvailable services: " + ids.toString());
    }

    private JsonObject getServices() {
        DiscoveryResolutionStu3 discoveryResolutionStu3 = new DiscoveryResolutionStu3(
                FhirContext.forDstu3().newRestfulGenericClient(HapiProperties.getServerAddress()));
        discoveryResolutionStu3.setMaxUriLength(this.getProviderConfiguration().getMaxUriLength());
        JsonArray services = discoveryResolutionStu3.resolve().getAsJson().getAsJsonArray("services");
        for (int i = 0; i < services.size(); i++) {
            JsonObject service = services.get(0).getAsJsonObject();
            PlanDefinition planDefinition = planDefinitionProvider.getDao().read(new IdType(service.get("id").getAsString()));

            if (planDefinition.hasExtension()) {
                List<Extension> extensionsL = planDefinition.getExtension();
                System.out.println("DEBUG: CdsServicesServlet::doGet - extensionsL.size() = " + extensionsL.size());

                Gson gsonExt = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.IDENTITY).setPrettyPrinting().create();
                String t = gsonExt.toJson(extensionsL);
                JsonParser parser = new JsonParser();
                JsonArray extensionsJsonL = parser.parse(t).getAsJsonArray();
                System.out.println("DEBUG: CdsServicesServlet::doGet - extensionsJsonL.size() = " + extensionsJsonL.size());

                JsonArray prettyExtJsonL = new JsonArray();
                for (JsonElement element : extensionsJsonL) {
                    JsonArray innerExtsJsonL = (JsonArray) ((JsonObject) element).get("extension");
                    JsonObject jsonObject = new JsonObject();
                    for (JsonElement configOpt : innerExtsJsonL) {
                        JsonObject innerUrlObj = (JsonObject) ((JsonObject) configOpt).get("url");
                        String innerUrlStr = innerUrlObj.get("myStringValue").getAsString();

                        JsonObject innerValObj = (JsonObject) ((JsonObject) configOpt).get("value");
                        String innerValStr = innerValObj.get("myStringValue").getAsString();

                        jsonObject.addProperty(innerUrlStr, innerValStr);
                    }
                    prettyExtJsonL.add(jsonObject);
                }
                JsonObject configObject = new JsonObject();
                configObject.add("pddi-configuration-items", prettyExtJsonL);

                service.add("extension", configObject);

//            if (!discovery.getItems().isEmpty()) {
//                JsonObject prefetchContent = new JsonObject();
//                for (DiscoveryItem item : discovery.getItems()) {
//                    prefetchContent.addProperty(item.getItemNo(), item.getUrl());
//                }
//                service.add("prefetch", prefetchContent);
//            }
            }
        }

        JsonObject serviceObject = new JsonObject();
        serviceObject.add("services", services);
        return serviceObject;

    }

    private String toJsonResponse(List<CdsCard> cards) {
        JsonObject ret = new JsonObject();
        JsonArray cardArray = new JsonArray();

        for (CdsCard card : cards) {
            cardArray.add(card.toJson());
        }

        ret.add("cards", cardArray);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(ret);
    }

    private void setAccessControlHeaders(HttpServletResponse resp) {
        if (HapiProperties.getCorsEnabled()) {
            resp.setHeader("Access-Control-Allow-Origin", HapiProperties.getCorsAllowedOrigin());
            resp.setHeader("Access-Control-Allow-Methods",
                    String.join(", ", Arrays.asList("GET", "HEAD", "POST", "OPTIONS")));
            resp.setHeader("Access-Control-Allow-Headers", String.join(", ", Arrays.asList("x-fhir-starter", "Origin",
                    "Accept", "X-Requested-With", "Content-Type", "Authorization", "Cache-Control")));
            resp.setHeader("Access-Control-Expose-Headers",
                    String.join(", ", Arrays.asList("Location", "Content-Location")));
            resp.setHeader("Access-Control-Max-Age", "86400");
        }
    }
}