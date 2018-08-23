package com.luxoft.blockchainlab.corda.hyperledger.indy.flow

import co.paralleluniverse.fibers.Suspendable
import com.luxoft.blockchainlab.corda.hyperledger.indy.service.IndyArtifactsRegistry
import com.luxoft.blockchainlab.hyperledger.indy.SchemaDetails
import com.luxoft.blockchainlab.hyperledger.indy.utils.SerializationUtils
import net.corda.core.flows.FlowException
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.CordaX500Name

/**
 * Flows to create an Indy scheme
 * */
object CreateSchemaFlow {

    /**
     * A flow to create an Indy scheme and register it with an artifact registry [artifactoryName]
     *
     * @param schemaName        name of the new schema
     * @param schemaVersion     version of the schema
     * @param schemaAttributes  a list of attribute names
     * @returns                 Schema ID
     * */
    @InitiatingFlow
    @StartableByRPC
    class Authority(
            private val schemaName: String,
            private val schemaVersion: String,
            private val schemaAttributes: List<String>,
            private val artifactoryName: CordaX500Name
    ) : FlowLogic<String>() {

        @Suspendable
        override fun call(): String {
            try {
                val schemaDetails = SchemaDetails(schemaName, schemaVersion, indyUser().did)

                val checkReq = IndyArtifactsRegistry.CheckRequest(
                        IndyArtifactsRegistry.ARTIFACT_TYPE.Schema, schemaDetails.filter
                )
                val isExist = subFlow(ArtifactsRegistryFlow.ArtifactVerifier(checkReq, artifactoryName))

                return if (isExist) {
                    // return schema id from ArtifactsRegistry
                    getSchemaId(schemaDetails, artifactoryName)
                } else {
                    // create new schema and add it to ArtifactsRegistry
                    val schema = indyUser().createSchema(schemaName, schemaVersion, schemaAttributes)
                    val schemaJson = SerializationUtils.anyToJSON(schema)

                    // put schema on Artifactory
                    val schemaReq = IndyArtifactsRegistry.PutRequest(
                            IndyArtifactsRegistry.ARTIFACT_TYPE.Schema, schemaJson
                    )
                    subFlow(ArtifactsRegistryFlow.ArtifactCreator(schemaReq, artifactoryName))

                    schema.id
                }

            } catch (t: Throwable) {
                logger.error("", t)
                throw FlowException(t.message)
            }
        }
    }
}