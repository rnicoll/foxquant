// $Id: ContractKey.java 685 2009-11-08 01:12:26Z  $
package org.lostics.foxquant.ib;

import com.ib.client.Contract;

/**
 * Primary key for contracts that doesn't take into account fields that may
 * change after contract details returns. Used to allow matching of
 * contract consumer to contract.
 */
class ContractKey extends Object {
    private final Contract contract;

    protected   ContractKey(final Contract setContract) {
        this.contract = setContract;

        if (!setContract.m_secType.equals(ConnectionManager.CONTRACT_SECURITY_TYPE_CASH)) {
            throw new IllegalArgumentException("Unsupported contract type \""
                + this.contract.m_secType + "\"; only "
                + ConnectionManager.CONTRACT_SECURITY_TYPE_CASH + " is supported at the moment.");
        }
    }

    public  boolean equals(final Object o) {
        final ContractKey contractKeyB = (ContractKey)o;
        final Contract contractB = contractKeyB.contract;

        if (this.contract.m_secType.equals(ConnectionManager.CONTRACT_SECURITY_TYPE_CASH) &&
            contractB.m_secType.equals(ConnectionManager.CONTRACT_SECURITY_TYPE_CASH)) {
            return this.contract.m_symbol.equals(contractB.m_symbol) &&
                this.contract.m_currency.equals(contractB.m_currency);
        }

        return false;
    }

    public  int hashCode() {
        int hash = 1;

        hash = hash * 31 + this.contract.m_secType.hashCode();
        hash = hash * 31 + this.contract.m_symbol.hashCode();
        hash = hash * 31 + this.contract.m_currency.hashCode();

        return hash;
    }

    public  String  toString() {
        if (this.contract.m_secType.equals(ConnectionManager.CONTRACT_SECURITY_TYPE_CASH)) {
            return this.contract.m_symbol + "/"
                + this.contract.m_currency;
        }
        return this.contract.toString();
    }
}
