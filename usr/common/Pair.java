package usr.common;

public class Pair<A extends Comparable<A>, B extends Comparable<B>> implements Comparable<Pair<A, B> > {
    private A first;
    private B second;

    public Pair(A first, B second) {
        super();
        this.first = first;
        this.second = second;
    }

     @Override
	public int compareTo(Pair<A, B> other) {
         return 0;
     }

    @Override
	public int hashCode() {
        int hashFirst = first != null ? first.hashCode() : 0;
        int hashSecond = second != null ? second.hashCode() : 0;

        hashFirst = hashFirst << 16;

        // could use first.hashCode() ^ second.hashCode() ;
        // from com.sun.corba.se.spi.orb.StringPair

        return (hashFirst + hashSecond) * hashSecond + hashFirst;

    }

    @Override
	public boolean equals(Object other) {
        if (other instanceof Pair) {
            Pair<?, ?> otherPair = (Pair <?,?>)other;
            return
                ((  this.first == otherPair.first ||
                    ( this.first != null && otherPair.first != null &&
                      this.first.equals(otherPair.first))) &&
                 (      this.second == otherPair.second ||
                        ( this.second != null && otherPair.second != null &&
                          this.second.equals(otherPair.second))) );
        }

        return false;
    }

    @Override
	public String toString() {
        return "(" + first + ", " + second + ")";
    }

    public A getFirst() {
        return first;
    }

    public void setFirst(A first) {
        this.first = first;
    }

    public B getSecond() {
        return second;
    }

    public void setSecond(B second) {
        this.second = second;
    }

}
