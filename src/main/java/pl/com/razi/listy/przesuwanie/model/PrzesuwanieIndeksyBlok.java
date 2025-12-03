package pl.com.razi.listy.przesuwanie.model;

import java.util.Objects;

public class PrzesuwanieIndeksyBlok {

	public final int start;
	public final int end;

	public PrzesuwanieIndeksyBlok(int start, int end) {
		this.start = start;
		this.end = end;
	}

	@Override
	public int hashCode() {
		return Objects.hash(end, start);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PrzesuwanieIndeksyBlok other = (PrzesuwanieIndeksyBlok) obj;
		return end == other.end && start == other.start;
	}

}
