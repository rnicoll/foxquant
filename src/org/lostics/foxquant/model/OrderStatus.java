// $Id: OrderStatus.java 685 2009-11-08 01:12:26Z  $
package org.lostics.foxquant.model;

public enum OrderStatus {
    PendingSubmit,
    PendingCancel,
    PreSubmitted,
    Submitted,
    ApiCancelled,
    Cancelled,
    Filled
}
