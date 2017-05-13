package ru.l2gw.gameserver.network;

import ru.l2gw.extensions.ccpGuard.crypt.ProtectionCrypt;
import ru.l2gw.util.Util;

public class GameCrypt
{
	private final byte[] _inKey = new byte[16];
	private final byte[] _outKey = new byte[16];
	private final ProtectionCrypt enc = new ProtectionCrypt();
	private final ProtectionCrypt dec = new ProtectionCrypt();
	private boolean _isEnabled;
	private boolean useprotect = false;

	public void setKey(byte[] key)
	{
		System.arraycopy(key, 0, _inKey, 0, 16);
		System.arraycopy(key, 0, _outKey, 0, 16);
	}

	public void setKey(byte[] key, boolean _useprotect)
	{
		setKey(key);
		useprotect = _useprotect;
		if(_useprotect)
		{
			byte[] newKey= new byte[8*4];
			for (int i=0; i<8; i++)
			{
				int val = ProtectionCrypt.getValue(key[i] & 0xFF);
				Util.intToBytes(val, newKey, i*4);
			}
			dec.setKey(newKey);
			enc.setKey(key);
		}
	}

	private boolean authLogin = true;

	public void decrypt(byte[] raw, final int offset, final int size)
	{
		if(!_isEnabled)
			return;

		if(authLogin && useprotect)
		{
			authLogin = false;
			byte[] auth = new byte[size];
			System.arraycopy(raw, offset, auth, 0, size);
			dec.decrypt(raw, offset, raw, offset, size);

			if((raw[offset] & 0xFF) == 0x2b)
				return;

			System.arraycopy(auth, 0, raw, offset, size);
			useprotect = false;
		}

		if(useprotect)
			dec.decrypt(raw, offset, raw, offset, size);
		else
		{
			int temp = 0;
			for(int i = 0; i < size; i++)
			{
				int temp2 = raw[offset + i] & 0xFF;
				raw[offset + i] = (byte) (temp2 ^ _inKey[i & 15] ^ temp);
				temp = temp2;
			}
			Util.intToBytes(Util.bytesToInt(_inKey, 8) + size, _inKey, 8);
		}
	}

	public void encrypt(byte[] raw, final int offset, final int size)
	{
		if(!_isEnabled)
		{
			_isEnabled = true;
			return;
		}

		if(useprotect)
			enc.encrypt(_outKey, 0, _outKey, 0, _outKey.length);

		int temp = 0;
		for(int i = 0; i < size; i++)
		{
			int temp2 = raw[offset + i] & 0xFF;
			temp = temp2 ^ _outKey[i & 15] ^ temp;
			raw[offset + i] = (byte) temp;
		}
		Util.intToBytes(Util.bytesToInt(_outKey, 8) + size, _outKey, 8);
	}
}