package ru.l2gw.loginserver.serverpackets;

/**
 * Fromat: d
 * d: the failure reason
 */
public final class LoginFail extends L2LoginServerPacket
{
	public static enum LoginFailReason
	{
		REASON_NO_MESSAGE(0x00),
		REASON_SYSTEM_ERROR(0x01), //There is a system error. Please log in again later
		REASON_PASS_WRONG(0x02), //The password you have entered is incorrect. Confirm your ...
		REASON_USER_OR_PASS_WRONG(0x03),
		REASON_ACCESS_FAILED_TRYA1(0x04), //Access failed. Please try again later.
		REASON_ACCOUNT_INFO_INCORR(0x05), //Your account information is incorrect. For more details ...
		REASON_ACCESS_FAILED_TRYA2(0x06), //Access failed. Please try again later.
		REASON_ACCOUNT_IN_USE(0x07), //Account is already in use. Unable to log in.
		REASON_MIN_AGE(0x0c), //Lineage II game services may be used by individuals 15 years of age or older ...
		REASON_SERVER_MAINTENANCE(0x10), //Currently undergoing game server maintenance. Please log in again later
		REASON_CHANGE_TEMP_PASS(0x11), //Please login after changing your temporary password.
		REASON_USAGE_TEMP_EXPIRED(0x12), //Your usage term has expired. PlayNC website ...
		REASON_TIME_LEFT_EXPIRED(0x13), //There is no time left on this account.
		REASON_SYS_ERR(0x14), //System Error.
		REASON_ACCESS_FAILED(0x15), //Access Filed.
		REASON_ATTEMPTED_RESTRICTED_IP(0x16), //Game connection attempted through a restricted IP.
		REASON_WEEK_USAGE_TIME_END(0x1e), //This week's usage time has finished.
		REASON_SECURITY_CARD_NUMB_I(0x1f), //The security card number is invalid.
		REASON_VERIFY_AGE(0x20), //Users who have not verified their age may not log in ...
		REASON_CANNOT_ACC_COUPON(0x21), //This server cannot be accessed by the coupon you are using.
		REASON_DUAL_BOX(0x23),
		REASON_ACCOUNT_INACTIVE(0x24), //Your account is currently inactive because you have not logged ...
		REASON_USER_AGREEMENT_DIS(0x25), //You must accept the User Agreement before this account ...
		REASON_GUARDIAN_CONSENT_REQ(0x26), //A guardian's consent is required before this account ...
		REASON_USER_AGREEMENT_DEC(0x27), //This account has declined the User Agreement or is pending ...
		REASON_ACCOUNT_SUSPENDED(0x28), //This account has been suspended ...
		REASON_CHANGE_PASS_AND_QUIZ(0x29), //Your account can only be used after changing your password and quiz ...
		REASON_LOGGED_INTO_10_ACCS(0x2a); //You are currently logged into 10 of your accounts and can no longer ...

		private final int _code;

		LoginFailReason(int code)
		{
			_code = code;
		}

		public final int getCode()
		{
			return _code;
		}
	}

	private int reason_code;

	public LoginFail(LoginFailReason reason)
	{
		reason_code = reason.getCode();
	}

	@Override
	protected void write()
	{
		writeC(0x01);
		writeD(reason_code);
	}
}
