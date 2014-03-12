package org.jatha.extras;

import org.jatha.Jatha;
import org.jatha.Registrar;
import org.jatha.compile.*;
import org.jatha.dynatype.LispPackage;
import org.jatha.compile.LispPrimitive;
import org.jatha.dynatype.LispValue;
import org.jatha.machine.SECDMachine;

public class UNSORTED implements Registrar {

	@Override
	public void Register(LispCompiler compiler) {
		final LispPackage SYSTEM_PKG = (LispPackage)compiler.getLisp().findPackage("SYSTEM");
		final Jatha f_lisp = compiler.getLisp();

		compiler.Register(new LispPrimitive(f_lisp, "ABS", 1) {
			public LispValue Execute(LispValue arg) {
				return arg.abs();
			}
		}, SYSTEM_PKG);
		compiler.Register(new LispPrimitive(f_lisp, "MOD", 2) {
			public LispValue Execute(LispValue x, LispValue n)
			{
				return x.mod(n);
			}
		}, SYSTEM_PKG);
		
		// Additional embedded primitives for perfomance improvement
/*		compiler.Register(new LispPrimitive(f_lisp, "1+", 1) {
			public LispValue Execute(LispValue arg) {
				return arg.add(f_lisp.makeCons(f_lisp.ONE, f_lisp.NIL));
			}
		}, SYSTEM_PKG);
		compiler.Register(new LispPrimitive(f_lisp, "1-", 1) {
			public LispValue Execute(LispValue arg) {
				return arg.sub(f_lisp.makeCons(f_lisp.ONE, f_lisp.NIL));
			}
		}, SYSTEM_PKG);*/
		
		
		compiler.Register(new ComplexLispPrimitive(f_lisp, "MIN", 1, Long.MAX_VALUE) {
			public LispValue Execute(LispValue args) {
				return args.car().min(args.cdr());
			}
		}, SYSTEM_PKG);
		compiler.Register(new ComplexLispPrimitive(f_lisp, "MAX", 1, Long.MAX_VALUE) {
			public LispValue Execute(LispValue args) {
				return args.car().max(args.cdr());
			}
		}, SYSTEM_PKG);
		
		// More complex functions
		
		
    compiler.Register(new AppendPrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new ApplyPrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new AproposPrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new ArrayDimensionsPrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new ArraypPrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new ArefPrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new AssocPrimitive(f_lisp),SYSTEM_PKG);
    // compiler.Register(new BackquotePrimitive(f_lisp), (LispPackage)f_lisp.findPackage("SYSTEM"));
    	compiler.Register(new BoundpPrimitive(f_lisp),SYSTEM_PKG);
		compiler.Register(new ButlastPrimitive(f_lisp),SYSTEM_PKG);
		compiler.Register(new CeilingPrimitive(f_lisp),SYSTEM_PKG);
		compiler.Register(new CharacterpPrimitive(f_lisp),SYSTEM_PKG);
		compiler.Register(new ClrhashPrimitive(f_lisp),SYSTEM_PKG);
    
    compiler.Register(new ConspPrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new ConstantpPrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new CopyListPrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new DefconstantPrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new DefparameterPrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new DefvarPrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new DegreesToRadiansPrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new DocumentationPrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new SetfDocumentationPrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new EqlPrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new EqualNumericPrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new ExitPrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new EvalPrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new ExptPrimitive(f_lisp),SYSTEM_PKG);    
    compiler.Register(new FactorialPrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new FboundpPrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new FindPackagePrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new MakepackagePrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new UsePackagePrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new PackageUseListPrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new PackageNamePrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new PackageNicknamesPrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new ImportPrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new ExportPrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new ShadowPrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new ShadowingImportPrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new FloorPrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new FuncallPrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new FunctionPrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new GrindefPrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new GethashPrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new GoPrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new GreaterThanPrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new GreaterThanOrEqualPrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new SetfArefPrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new SetfGethashPrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new HashtablepPrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new HashtableCountPrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new HashtableRehashSizePrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new HashtableRehashThresholdPrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new HashtableSizePrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new HashtableTestPrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new InternPrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new KeywordpPrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new LastPrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new LengthPrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new LessThanPrimitive(f_lisp),SYSTEM_PKG);
   	compiler.Register(new LessThanOrEqualPrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new ListAllPackagesPrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new ListpPrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new LoadPrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new LoadFromJarPrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new Macroexpand1Primitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new MacroexpandPrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new MakeArrayPrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new MakeHashTablePrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new MemberPrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new NconcPrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new NreversePrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new PopPrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new PositionPrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new Prin1Primitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new PrincPrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new PrintPrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new PushPrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new RadiansToDegreesPrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new RassocPrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new ReciprocalPrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new RemhashPrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new RemovePrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new RestPrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new ReturnFromPrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new ReversePrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new RplacaPrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new RplacdPrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new SetPrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new SetfCarPrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new SetfCdrPrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new SetfSymbolFunctionPrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new SetfSymbolPlistPrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new SetfSymbolValuePrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new SetqPrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new SquareRootPrimitive(f_lisp),SYSTEM_PKG);
    
	compiler.Register(new LispPrimitive(f_lisp, "STRING", 1) {
		public LispValue Execute(LispValue arg) {
			return arg.string();
		}
	}, SYSTEM_PKG);
	compiler.Register(new LispPrimitive(f_lisp, "STRING-UPCASE", 1) {
		public LispValue Execute(LispValue arg) {
			return arg.stringUpcase();
		}
	}, SYSTEM_PKG);
    

	compiler.Register(new SubstPrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new SymbolpPrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new SymbolFunctionPrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new SymbolNamePrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new SymbolPackagePrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new SymbolPlistPrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new SymbolValuePrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new TagbodyPrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new ZeropPrimitive(f_lisp),SYSTEM_PKG);

    compiler.Register(new TracePrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new GcPrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new GcFullPrimitive(f_lisp),SYSTEM_PKG);
    compiler.Register(new FreePrimitive(f_lisp),SYSTEM_PKG);
    
    	// "inline" primitives (for perfomance purposes)
		compiler.Register(new InlineLispPrimitive(f_lisp, "BLOCK", 1, Long.MAX_VALUE) {
			public LispValue CompileArgs(final LispCompiler compiler, final SECDMachine machine, final LispValue args, final LispValue valueList, final LispValue code)
					throws CompilerException
			{
				final LispValue tag = args.car();
				compiler.getLegalBlocks().push(tag);
				final LispValue fullCode = f_lisp.makeList(f_lisp.makeCons(f_lisp.getEval().intern("PROGN"),args.cdr()));
				final LispValue compiledCode = compiler.compileArgsLeftToRight(fullCode, valueList, f_lisp.makeCons(machine.BLK, f_lisp.makeCons(tag, code)));
				compiler.getLegalBlocks().pop();
				return compiledCode;
			}
		}, SYSTEM_PKG);
		compiler.Register(new InlineLispPrimitive(f_lisp, "LIST", 0, Long.MAX_VALUE) {
			public LispValue CompileArgs(final LispCompiler compiler, final SECDMachine machine, final LispValue args, final LispValue valueList, final LispValue code)
					throws CompilerException
			{
				return compiler.compileArgsLeftToRight(args, valueList,
						f_lisp.makeCons(machine.LIS,
								f_lisp.makeCons(args.length(), code)));
			}
		}, SYSTEM_PKG);
		compiler.Register(new InlineLispPrimitive(f_lisp, "LIST*", 1, Long.MAX_VALUE) {
			LispValue CONS = new ConsPrimitive(f_lisp);
			public LispValue CompileArgs(final LispCompiler compiler, final SECDMachine machine, final LispValue args, final LispValue valueList, final LispValue code)
					throws CompilerException
			{
				if (args.cdr() == f_lisp.NIL)
					return compiler.compileArgsLeftToRight(args, valueList, code);
				return compiler.compile(args.car(), valueList,
						CompileArgs(compiler, machine, args.cdr(),
								valueList,
								f_lisp.makeCons(CONS, code)));
			}
		}, SYSTEM_PKG);
	}
}
