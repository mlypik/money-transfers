package io.github.mlypik

/**
 * Known errors, invariant violations, business logic violations
 */
object errors {
  sealed abstract class TransferError(msg: String) extends Exception(msg)

  case object AccountNotFound extends TransferError("Account not found")
  case object BalanceUpdateFailed extends TransferError("Balance update failed")
  case object OverdrawViolation extends TransferError("Attempted to overdraw")

}
