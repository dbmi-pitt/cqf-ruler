JAVA_HOME=/Library/Java/JavaVirtualMachines/adoptopenjdk-8.jdk/Contents/Home/

CQL_TO_ELM_BASE=/Users/erc53/Github/clinical_quality_language/Src/java/cql-to-elm/build/install/cql-to-elm/bin


FHIR_SERVER_BASE_URL=http://localhost:8080/cqf-ruler-dstu3/fhir

PDDI_EXAMPLE_BASE=/Users/erc53/Github/cqf-ruler/examples/pddi-cds
TAG_UPLOAD_BASE=/Users/erc53/Github/cqf-ruler/tag-uploader/tag-uploader

TERMINOLOGY_TAG_UPLOAD=/Users/erc53/Github/cqf-ruler/tag-uploader/tag-uploader/terminology

compile-elm-warfarin-nsaids-sign:
	JAVA_HOME=${JAVA_HOME} ${CQL_TO_ELM_BASE}/cql-to-elm --input ${PDDI_EXAMPLE_BASE}/warfarin-nsaids-order-sign/warfarin-nsaids-cds-sign-logic.cql --output ${PDDI_EXAMPLE_BASE}/warfarin-nsaids-order-sign
	sed "s/@ELM_BASE64_DATA/`base64 -w 100000000 ${PDDI_EXAMPLE_BASE}/warfarin-nsaids-order-sign/warfarin-nsaids-cds-sign-logic.xml`/g" ${PDDI_EXAMPLE_BASE}/warfarin-nsaids-order-sign/library-bundle.template | sed "s/@CQL_BASE64_DATA/`base64 -w 100000000 ${PDDI_EXAMPLE_BASE}/warfarin-nsaids-order-sign/warfarin-nsaids-cds-sign-logic.cql`/g" > ${PDDI_EXAMPLE_BASE}/warfarin-nsaids-order-sign/library-bundle.json

compile-elm-warfarin-nsaids-select:
	JAVA_HOME=${JAVA_HOME} ${CQL_TO_ELM_BASE}/cql-to-elm --input ${PDDI_EXAMPLE_BASE}/warfarin-nsaids-order-select/warfarin-nsaids-cds-select-logic.cql --output ${PDDI_EXAMPLE_BASE}/warfarin-nsaids-order-select
	sed "s/@ELM_BASE64_DATA/`base64 -w 100000000 ${PDDI_EXAMPLE_BASE}/warfarin-nsaids-order-select/warfarin-nsaids-cds-select-logic.xml`/g" ${PDDI_EXAMPLE_BASE}/warfarin-nsaids-order-select/library-bundle.template | sed "s/@CQL_BASE64_DATA/`base64 -w 100000000 ${PDDI_EXAMPLE_BASE}/warfarin-nsaids-order-select/warfarin-nsaids-cds-select-logic.cql`/g" > ${PDDI_EXAMPLE_BASE}/warfarin-nsaids-order-select/library-bundle.json

compile-elm-digoxin-cyclosporine-sign:
	JAVA_HOME=${JAVA_HOME} ${CQL_TO_ELM_BASE}/cql-to-elm --input ${PDDI_EXAMPLE_BASE}/digoxin-cyclosporine-order-sign/digoxin-cyclosporine-cds-sign-logic.cql --output ${PDDI_EXAMPLE_BASE}/digoxin-cyclosporine-order-sign
	sed "s/@ELM_BASE64_DATA/`base64 -w 100000000 ${PDDI_EXAMPLE_BASE}/digoxin-cyclosporine-order-sign/digoxin-cyclosporine-cds-sign-logic.xml`/g" ${PDDI_EXAMPLE_BASE}/digoxin-cyclosporine-order-sign/library-bundle.template | sed "s/@CQL_BASE64_DATA/`base64 -w 100000000 ${PDDI_EXAMPLE_BASE}/digoxin-cyclosporine-order-sign/digoxin-cyclosporine-cds-sign-logic.cql`/g" > ${PDDI_EXAMPLE_BASE}/digoxin-cyclosporine-order-sign/library-bundle.json

compile-elm-digoxin-cyclosporine-select:
	JAVA_HOME=${JAVA_HOME} ${CQL_TO_ELM_BASE}/cql-to-elm --input ${PDDI_EXAMPLE_BASE}/digoxin-cyclosporine-order-select/digoxin-cyclosporine-cds-select-logic.cql --output ${PDDI_EXAMPLE_BASE}/digoxin-cyclosporine-order-select
	sed "s/@ELM_BASE64_DATA/`base64 -w 100000000 ${PDDI_EXAMPLE_BASE}/digoxin-cyclosporine-order-select/digoxin-cyclosporine-cds-select-logic.xml`/g" ${PDDI_EXAMPLE_BASE}/digoxin-cyclosporine-order-select/library-bundle.template | sed "s/@CQL_BASE64_DATA/`base64 -w 100000000 ${PDDI_EXAMPLE_BASE}/digoxin-cyclosporine-order-select/digoxin-cyclosporine-cds-select-logic.cql`/g" > ${PDDI_EXAMPLE_BASE}/digoxin-cyclosporine-order-select/library-bundle.json

compile-elm-warfarin-nsaids-prescribe:
	JAVA_HOME=${JAVA_HOME} ${CQL_TO_ELM_BASE}/cql-to-elm --input ${PDDI_EXAMPLE_BASE}/warfarin-nsaids-prescribe/warfarin-nsaids-cds-prescribe-logic.cql --output ${PDDI_EXAMPLE_BASE}/warfarin-nsaids-prescribe
	sed "s/@ELM_BASE64_DATA/`base64 -w 100000000 ${PDDI_EXAMPLE_BASE}/warfarin-nsaids-prescribe/warfarin-nsaids-cds-prescribe-logic.xml`/g" ${PDDI_EXAMPLE_BASE}/warfarin-nsaids-prescribe/library-bundle.template | sed "s/@CQL_BASE64_DATA/`base64 -w 100000000 ${PDDI_EXAMPLE_BASE}/warfarin-nsaids-prescribe/warfarin-nsaids-cds-prescribe-logic.cql`/g" > ${PDDI_EXAMPLE_BASE}/warfarin-nsaids-prescribe/library-bundle.json

load-fhir-helpers:
	node . -v -d ${TAG_UPLOAD_BASE}/bhin-phi-fhir-helper-and-termdef -S ${FHIR_SERVER_BASE_URL}

load-pddi-cds-configuration-options:
	curl -X POST --header "Content-Type:application/xml" -d @${PDDI_EXAMPLE_BASE}/pddi-cds-config-options-extension-bundle.xml ${FHIR_SERVER_BASE_URL}

load-warfarin-nsaids-value-sets:
	curl -X POST --header "Content-Type:application/xml" -d @${TERMINOLOGY_TAG_UPLOAD}/warfarin-nsaids-valueset-bundle.xml ${FHIR_SERVER_BASE_URL}

load-warfarin-nsaids-prescribe:
	cp ${PDDI_EXAMPLE_BASE}/warfarin-nsaids-prescribe/*json ${TAG_UPLOAD_BASE}/warfarin-nsaids-prescribe
	rm ${TAG_UPLOAD_BASE}/warfarin-nsaids-prescribe/warfarin-nsaids-cds-request.json
	node . -v -d ${TAG_UPLOAD_BASE}/warfarin-nsaids-prescribe -S ${FHIR_SERVER_BASE_URL}
	sleep 1
	node . -v -d ${TAG_UPLOAD_BASE}/warfarin-nsaids-prescribe -S ${FHIR_SERVER_BASE_URL}


load-warfarin-nsaids-order-sign:
	cp ${PDDI_EXAMPLE_BASE}/warfarin-nsaids-order-sign/*json ${TAG_UPLOAD_BASE}/warfarin-nsaids-order-sign
	rm ${TAG_UPLOAD_BASE}/warfarin-nsaids-order-sign/warfarin-nsaids-cds-request.json
	rm ${TAG_UPLOAD_BASE}/warfarin-nsaids-order-sign/warfarin-nsaids-cds-multiple-1-request.json
	rm ${TAG_UPLOAD_BASE}/warfarin-nsaids-order-sign/warfarin-nsaids-cds-multiple-2-request.json
	node . -v -d ${TAG_UPLOAD_BASE}/warfarin-nsaids-order-sign -S ${FHIR_SERVER_BASE_URL}
	sleep 1
	node . -v -d ${TAG_UPLOAD_BASE}/warfarin-nsaids-order-sign -S ${FHIR_SERVER_BASE_URL}


load-warfarin-nsaids-order-select:
	cp ${PDDI_EXAMPLE_BASE}/warfarin-nsaids-order-select/*json ${TAG_UPLOAD_BASE}/warfarin-nsaids-order-select/
	rm ${TAG_UPLOAD_BASE}/warfarin-nsaids-order-select/warfarin-nsaids-cds-request.json
	rm ${TAG_UPLOAD_BASE}/warfarin-nsaids-order-select/warfarin-nsaids-cds-multiple-1-request.json
	rm ${TAG_UPLOAD_BASE}/warfarin-nsaids-order-select/warfarin-nsaids-cds-multiple-2-request.json
	node . -v -d ${TAG_UPLOAD_BASE}/warfarin-nsaids-order-select -S ${FHIR_SERVER_BASE_URL}
	sleep 1
	node . -v -d ${TAG_UPLOAD_BASE}/warfarin-nsaids-order-select -S ${FHIR_SERVER_BASE_URL}

load-digoxin-cyclosporine-value-sets:
	curl -X POST --header "Content-Type:application/xml" -d @${TERMINOLOGY_TAG_UPLOAD}/digoxin-cyclosporine-valueset-bundle.xml ${FHIR_SERVER_BASE_URL}

load-digoxin-cyclosporine-order-select:
	cp ${PDDI_EXAMPLE_BASE}/digoxin-cyclosporine-order-select/*json ${TAG_UPLOAD_BASE}/digoxin-cyclosporine-order-select/
	rm ${TAG_UPLOAD_BASE}/digoxin-cyclosporine-order-select/digoxin-cyclosporine-cds-request.json
	node . -v -d ${TAG_UPLOAD_BASE}/digoxin-cyclosporine-order-select -S ${FHIR_SERVER_BASE_URL}
	sleep 1
	node . -v -d ${TAG_UPLOAD_BASE}/digoxin-cyclosporine-order-select -S ${FHIR_SERVER_BASE_URL}

load-digoxin-cyclosporine-order-sign:
	cp ${PDDI_EXAMPLE_BASE}/digoxin-cyclosporine-order-sign/*json ${TAG_UPLOAD_BASE}/digoxin-cyclosporine-order-sign/
	rm ${TAG_UPLOAD_BASE}/digoxin-cyclosporine-order-sign/digoxin-cyclosporine-cds-request.json
	node . -v -d ${TAG_UPLOAD_BASE}/digoxin-cyclosporine-order-sign -S ${FHIR_SERVER_BASE_URL}
	sleep 1
	node . -v -d ${TAG_UPLOAD_BASE}/digoxin-cyclosporine-order-sign -S ${FHIR_SERVER_BASE_URL}



