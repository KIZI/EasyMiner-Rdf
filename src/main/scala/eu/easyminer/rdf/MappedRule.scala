package eu.easyminer.rdf

import com.github.propi.rdfrules.data.TripleItem
import com.github.propi.rdfrules.index.TripleItemHashIndex
import com.github.propi.rdfrules.rule.{Atom, Measure, Rule}
import com.github.propi.rdfrules.stringifier.Stringifier
import com.github.propi.rdfrules.utils.TypedKeyMap
import eu.easyminer.rdf.MappedRule.MappedAtom
import com.github.propi.rdfrules.stringifier.CommonStringifiers._

import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 9. 4. 2018.
  */
case class MappedRule(text: String, head: MappedAtom, body: IndexedSeq[MappedAtom], measures: TypedKeyMap.Immutable[Measure])

object MappedRule {

  case class MappedAtom(subject: MappedAtomItem, predicate: TripleItem.Uri, `object`: MappedAtomItem)

  object MappedAtom {
    implicit def apply(atom: Atom)(implicit mapper: TripleItemHashIndex): MappedAtom = new MappedAtom(atom.subject, mapper.getTripleItem(atom.predicate).asInstanceOf[TripleItem.Uri], atom.`object`)
  }

  sealed trait MappedAtomItem

  object MappedAtomItem {

    case class Variable(variable: Atom.Variable) extends MappedAtomItem

    case class Constant(tripleItem: TripleItem) extends MappedAtomItem

    implicit def apply(atomItem: Atom.Item)(implicit mapper: TripleItemHashIndex): MappedAtomItem = atomItem match {
      case x: Atom.Variable => Variable(x)
      case Atom.Constant(x) => Constant(mapper.getTripleItem(x))
    }

  }

  implicit class PimpedSimpleRule(rule: Rule.Simple)(implicit mapper: TripleItemHashIndex) {
    def toMappedRule: MappedRule = MappedRule(Stringifier(rule.asInstanceOf[Rule]), rule.head, rule.body.map(MappedAtom.apply), rule.measures)
  }

}
