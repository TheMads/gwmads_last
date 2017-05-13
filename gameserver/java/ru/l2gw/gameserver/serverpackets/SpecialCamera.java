package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.L2Character;

public class SpecialCamera extends L2GameServerPacket
{
	private int _id;
	private int _dist;
	private int _yaw;
	private int _pitch;
	private int _time;
	private int _duration;
	private int _finish_angle_h;
	private int _finish_angle_v;
	private int _moveMode;
	private int _unk;
	private int _start;

	public SpecialCamera(int id, int dist, int yaw, int pitch, int time, int duration)
	{
		this(id, dist, yaw, pitch, time, duration, 0, 0, true, false);
	}

	public SpecialCamera(L2Character obj, int dist, int yaw, int pitch, int time, int duration, int finish_angle_h, int finish_angle_v, boolean movieMode, boolean start)
	{
		this(obj.getObjectId(), dist, yaw, pitch, time, duration, finish_angle_h, finish_angle_v, movieMode, start);
	}

	public SpecialCamera(int id, int dist, int yaw, int pitch, int time, int duration, int finish_angle_h, int finish_angle_v, boolean movieMode, boolean start)
	{
		this(id, dist, yaw, pitch, time, duration, finish_angle_h, finish_angle_v, movieMode ? 1 : 0, 0, start ? 1 : 0);
	}

	public SpecialCamera(int id, int dist, int yaw, int pitch, int time, int duration, int finish_angle_h, int finish_angle_v, int movieMode, int unk, int start)
	{
		_id = id;
		_dist = dist;
		_yaw = yaw;
		_pitch = pitch;
		_time = time;
		_duration = duration;
		_finish_angle_h = finish_angle_h;
		_finish_angle_v = finish_angle_v;
		_moveMode = movieMode;
		_unk = unk;
		_start = start;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xd6);
		writeD(_id);
		writeD(_dist);
		writeD(_yaw);
		writeD(_pitch);
		writeD(_time);
		writeD(_duration);
		writeD(_finish_angle_h);
		writeD(_finish_angle_v);
		writeD(_moveMode);
		writeD(_unk); // Unknown
		writeD(_start);
	}
}