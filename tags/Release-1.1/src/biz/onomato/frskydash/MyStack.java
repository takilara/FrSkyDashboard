package biz.onomato.frskydash;

public class MyStack {
	private int _size;
	private int[] _stack;
	public MyStack(int Size)
	{
		_stack = new int[Size];
		_size=Size;
		reset();
	}
	
	public String toString()
	{
		String buf ="";
		for(int n=0;n<_size;n++)
		{
			buf +=_stack[n];
			if(n!=_size-1)
			{
				buf += ",";
			}
		}
		return buf;
	}
	
	public void reset()
	{
		for(int n=0;n<_size;n++)
		{
			_stack[n] = -1;
		}
	}
	
	public int push(int item)
	{
		for(int n=_size-2;n>=0;n--)
		{
			_stack[n+1] = _stack[n];
		}
		_stack[0] = item;
		return average();
	}
	
	public int average()
	{
		int _cnt = 0;
		int _sum = 0;
		for(int n=0;n<_stack.length;n++)
		{
			if(_stack[n]>=0)
			{
				_cnt++;
				_sum += _stack[n];
			}
		}
		int _avg = _sum/_cnt;
		return _avg;
	}
}
	
