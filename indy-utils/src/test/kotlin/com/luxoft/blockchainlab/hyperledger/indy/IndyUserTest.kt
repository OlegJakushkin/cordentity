package com.luxoft.blockchainlab.hyperledger.indy

import com.luxoft.blockchainlab.hyperledger.indy.utils.PoolManager
import com.luxoft.blockchainlab.hyperledger.indy.utils.SerializationUtils
import com.luxoft.blockchainlab.hyperledger.indy.utils.getRootCause
import org.hyperledger.indy.sdk.anoncreds.Anoncreds
import org.hyperledger.indy.sdk.wallet.Wallet
import org.hyperledger.indy.sdk.wallet.WalletExistsException
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File

class IndyUserTest {

    lateinit var indyUser: IndyUser
    lateinit var wallet: Wallet

    @Before
    fun setup() {
        val walletName = "default-wallet"
        val poolName = "default-pool"
        val credentials = """{"key": "key"}"""

        val walletConfig = SerializationUtils.anyToJSON(WalletConfig(walletName))

        try {
            Wallet.createWallet(walletConfig, credentials).get()
        } catch (ex: Exception) {
            if (getRootCause(ex) !is WalletExistsException) throw ex else logger.debug("Wallet already exists")
        }

        wallet = Wallet.openWallet(walletConfig, credentials).get()
        val pool = PoolManager.openIndyPool(PoolManager.defaultGenesisResource, poolName)
        indyUser = IndyUser(pool, wallet, null)
    }

    @After
    fun down() {
        wallet.closeWallet()
    }

    @Test
    fun `check schema id format wasnt changed`() {
        val name = "unitTestSchema"
        val version = "1.0"
        val utilsId = IndyUser.buildSchemaId(indyUser.did, name, version)

        val schemaInfo = Anoncreds.issuerCreateSchema(
                indyUser.did, name, version, """["attr1"]"""
        ).get()
        assert(utilsId == schemaInfo.schemaId) {"Generated schema ID doesn't match SDK' ID anymore"}
    }

    @Test
    fun `check definition id format wasnt changed`() {
        val schemaSeqNo = 14
        val utilsId = IndyUser.buildCredentialDefinitionId(indyUser.did, schemaSeqNo)

        val schemaJson = """{
            "ver":"1.0",
            "id":"V4SGRU86Z58d6TV7PBUe6f:2:schema_education:1.0",
            "name":"schema_education",
            "version":"1.0","attrNames":["attrY","attrX"],
            "seqNo":${schemaSeqNo}
        }"""

        val credDefInfo = Anoncreds.issuerCreateAndStoreCredentialDef(
                wallet, indyUser.did, schemaJson, IndyUser.TAG, IndyUser.SIGNATURE_TYPE, null
        ).get()
        assert(utilsId == credDefInfo.credDefId) {"Generated credDef ID doesn't match SDK' ID anymore"}
    }
}