package org.drools.model.consequences;

import static org.drools.model.functions.FunctionUtils.toFunctionN;

import java.util.ArrayList;
import java.util.List;

import org.drools.model.Consequence;
import org.drools.model.Drools;
import org.drools.model.RuleItemBuilder;
import org.drools.model.Variable;
import org.drools.model.functions.Block0;
import org.drools.model.functions.Block1;
import org.drools.model.functions.Block2;
import org.drools.model.functions.Block3;
import org.drools.model.functions.Block4;
import org.drools.model.functions.Block5;
import org.drools.model.functions.Block6;
import org.drools.model.functions.Block7;
import org.drools.model.functions.BlockN;
import org.drools.model.functions.Function0;
import org.drools.model.functions.Function1;
import org.drools.model.functions.Function2;
import org.drools.model.functions.Function3;
import org.drools.model.functions.Function4;
import org.drools.model.functions.Function5;
import org.drools.model.functions.Function6;
import org.drools.model.functions.FunctionN;
import org.drools.model.functions.ScriptBlock;

public class ConsequenceBuilder {

    public _0 execute(Block0 block) {
        return new _0(block);
    }

    public _0 execute(Block1<Drools> block) {
        return new _0(block);
    }

    public <A> _1<A> on(Variable<A> dec1) {
        return new _1(dec1);
    }

    public <A, B> _2<A, B> on(Variable<A> decl1, Variable<B> decl2) {
        return new _2(decl1, decl2);
    }

    public interface ValidBuilder extends RuleItemBuilder<Consequence> { }

    public static abstract class AbstractValidBuilder<T extends AbstractValidBuilder> implements ValidBuilder {
        private final Variable[] declarations;
        protected BlockN block;
        private List<FunctionN> inserts = new ArrayList<FunctionN>();
        private List<Consequence.Update> updates = new ArrayList<Consequence.Update>();
        private Variable[] deletes;
        protected boolean usingDrools = false;
        protected boolean breaking = false;
        protected String language = "java";

        protected AbstractValidBuilder(Variable... declarations) {
            this.declarations = declarations;
        }

        @Override
        public Consequence get() {
            return new ConsequenceImpl( block,
                                        declarations,
                                        inserts.toArray(new FunctionN[inserts.size()]),
                                        updates.toArray(new Consequence.Update[updates.size()]),
                                        deletes,
                                        usingDrools,
                                        breaking,
                                        language);
        }

        public AbstractValidBuilder update(Variable updatedVariable, String... updatedFields) {
            updates.add(new ConsequenceImpl.UpdateImpl(updatedVariable, updatedFields));
            return this;
        }

        public AbstractValidBuilder delete(Variable... deletes) {
            this.deletes = deletes;
            return this;
        }

        protected void addInsert(FunctionN f) {
            inserts.add(f);
        }

        public T breaking() {
            breaking = true;
            return (T) this;
        }
    }

    public static class _0 extends AbstractValidBuilder<_0> {
        public _0(final Block0 block) {
            super(new Variable[0]);
            this.block = block.asBlockN();
        }

        public _0(final Block1<Drools> block) {
            super();
            this.usingDrools = true;
            this.block = block.asBlockN();
        }

        public _0(String language, String script) {
            super();
            this.usingDrools = true;
            this.language = language;
            this.block = new ScriptBlock(script);
        }

        public <R> _0 insert(final Function0<R> f) {
            addInsert(toFunctionN(f));
            return this;
        }
    }

    public static class _1<A> extends AbstractValidBuilder<_1<A>> {
        public _1(Variable<A> declaration) {
            super(declaration);
        }

        public _1<A> execute(final Block1<A> block) {
            this.block = block.asBlockN();
            return this;
        }

        public _1<A> execute(final Block2<Drools, A> block) {
            this.usingDrools = true;
            this.block = block.asBlockN();
            return this;
        }

        public _1<A> executeScript(String language, String script) {
            this.usingDrools = true;
            this.language = language;
            this.block = new ScriptBlock(script);
            return this;
        }

        public <R> _1 insert(final Function1<A, R> f) {
            addInsert(toFunctionN(f));
            return this;
        }
    }

    public static class _2<A, B> extends AbstractValidBuilder<_2<A,B>> {
        public _2(Variable<A> decl1, Variable<B> decl2) {
            super(decl1, decl2);
        }

        public _2<A, B> execute(final Block2<A, B> block) {
            this.block = block.asBlockN();
            return this;
        }

        public _2<A, B> execute(final Block3<Drools, A, B> block) {
            this.usingDrools = true;
            this.block = block.asBlockN();
            return this;
        }

        public _2<A, B> executeScript(String language, String script) {
            this.usingDrools = true;
            this.language = language;
            this.block = new ScriptBlock(script);
            return this;
        }

        public <R> _2 insert(final Function2<A, B, R> f) {
            addInsert(toFunctionN(f));
            return this;
        }
    }

    public static class _3<A, B, C> extends AbstractValidBuilder<_3<A,B,C>> {
        public _3(Variable<A> decl1, Variable<B> decl2, Variable<C> decl3) {
            super(decl1, decl2, decl3);
        }

        public _3<A, B, C> execute(final Block3<A, B, C> block) {
            this.block = block.asBlockN();
            return this;
        }

        public _3<A, B, C> execute(final Block4<Drools, A, B, C> block ) {
            this.usingDrools = true;
            this.block = block.asBlockN();
            return this;
        }

        public _3<A, B, C> executeScript(String language, String script) {
            this.usingDrools = true;
            this.language = language;
            this.block = new ScriptBlock(script);
            return this;
        }

        public <R> _3 insert(final Function3<A, B, C, R> f) {
            addInsert(toFunctionN(f));
            return this;
        }
    }

    public static class _4<A, B, C, D> extends AbstractValidBuilder<_4<A,B,C,D>> {
        public _4(Variable<A> decl1, Variable<B> decl2, Variable<C> decl3, Variable<D> decl4) {
            super(decl1, decl2, decl3, decl4);
        }

        public _4<A, B, C, D> execute(final Block4<A, B, C, D> block) {
            this.block = block.asBlockN();
            return this;
        }

        public _4<A, B, C, D> execute(final Block5<Drools, A, B, C, D> block ) {
            this.usingDrools = true;
            this.block = block.asBlockN();
            return this;
        }

        public _4<A, B, C, D> executeScript(String language, String script) {
            this.usingDrools = true;
            this.language = language;
            this.block = new ScriptBlock(script);
            return this;
        }

        public <R> _4 insert(final Function4<A, B, C, D, R> f) {
            addInsert(toFunctionN(f));
            return this;
        }
    }

    public static class _5<A, B, C, D, E> extends AbstractValidBuilder<_5<A,B,C,D,E>> {
        public _5(Variable<A> decl1, Variable<B> decl2, Variable<C> decl3, Variable<D> decl4, Variable<E> decl5) {
            super(decl1, decl2, decl3, decl4, decl5);
        }

        public _5<A, B, C, D, E> execute(final Block5<A, B, C, D, E> block) {
            this.block = block.asBlockN();
            return this;
        }

        public _5<A, B, C, D, E> execute(final Block6<Drools, A, B, C, D, E> block ) {
            this.usingDrools = true;
            this.block = block.asBlockN();
            return this;
        }

        public _5<A, B, C, D, E> executeScript(String language, String script) {
            this.usingDrools = true;
            this.language = language;
            this.block = new ScriptBlock(script);
            return this;
        }

        public <R> _5 insert(final Function5<A, B, C, D, E, R> f) {
            addInsert(toFunctionN(f));
            return this;
        }
    }

    public static class _6<A, B, C, D, E, F> extends AbstractValidBuilder<_6<A,B,C,D,E,F>> {
        public _6(Variable<A> decl1, Variable<B> decl2, Variable<C> decl3, Variable<D> decl4, Variable<E> decl5, Variable<F> decl6) {
            super(decl1, decl2, decl3, decl4, decl5, decl6);
        }

        public _6<A, B, C, D, E, F> execute(final Block6<A, B, C, D, E, F> block) {
            this.block = block.asBlockN();
            return this;
        }

        public _6<A, B, C, D, E, F> execute(final Block7<Drools, A, B, C, D, E, F> block ) {
            this.usingDrools = true;
            this.block = block.asBlockN();
            return this;
        }

        public _6<A, B, C, D, E, F> executeScript(String language, String script) {
            this.usingDrools = true;
            this.language = language;
            this.block = new ScriptBlock(script);
            return this;
        }

        public <R> _6 insert(final Function6<A, B, C, D, E, F, R> f) {
            addInsert(toFunctionN(f));
            return this;
        }
    }

    public static class _N extends AbstractValidBuilder<_N> {

        public _N(Variable... declarations) {
            super(declarations);
        }

        public _N executeScript(String language, String script) {
            this.usingDrools = true;
            this.language = language;
            this.block = new ScriptBlock(script);
            return this;
        }

        public <R> _N insert(final FunctionN<R> f) {
            addInsert(f);
            return this;
        }
    }
}
