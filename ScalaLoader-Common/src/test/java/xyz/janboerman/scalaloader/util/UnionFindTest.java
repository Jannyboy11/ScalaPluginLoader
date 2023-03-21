package xyz.janboerman.scalaloader.util;

import org.junit.jupiter.api.Test;

public class UnionFindTest {

    private static <T> boolean invariant_AllRootsAreTheirOwnRep(UnionFind<T> uf) {
        return uf.representatives().stream().allMatch(root -> uf.getRepresentative(root).equals(root));
    }

    private static <T> boolean invariant_AllValuesHaveARep(UnionFind<T> uf) {
        return uf.values().stream().allMatch(value -> uf.representatives().contains(uf.getRepresentative(value)));
    }

    private static <T> boolean invariant_RepresentativesSubsetOfValues(UnionFind<T> uf) {
        return uf.values().containsAll(uf.representatives());
    }

    private static <T> boolean invariant(UnionFind<T> uf) {
        return invariant_RepresentativesSubsetOfValues(uf) && invariant_AllRootsAreTheirOwnRep(uf) && invariant_AllValuesHaveARep(uf);
    }

    @Test
    public void testBasic() {
        UnionFind<Integer> uf = new UnionFind<>();
        assert invariant(uf);
        uf.add(0);
        assert invariant(uf);
        uf.add(1);
        assert invariant(uf);
        uf.add(2);
        assert invariant(uf);
        uf.setParent(0, 1);
        assert invariant(uf);
        assert uf.getRepresentative(2).equals(2);
        assert uf.getRepresentative(0).equals(1);
        assert uf.getRepresentative(1).equals(1);
    }

    @Test
    public void testUnite() {
        UnionFind<Integer> uf = new UnionFind<>();

        uf.setParent(0, 1);
        uf.setParent(2, 3);

        assert uf.getRepresentative(0).equals(1);
        assert uf.getRepresentative(1).equals(1);
        assert uf.getRepresentative(2).equals(3);
        assert uf.getRepresentative(3).equals(3);

        assert invariant(uf);

        uf.unite(0, 2);

        assert invariant(uf);

        assert uf.getParent(1).equals(uf.getParent(3));
        Integer rep = uf.getParent(1);
        assert uf.getRepresentative(0).equals(rep);
        assert uf.getRepresentative(1).equals(rep);
        assert uf.getRepresentative(2).equals(rep);
        assert uf.getRepresentative(3).equals(rep);
    }

}
