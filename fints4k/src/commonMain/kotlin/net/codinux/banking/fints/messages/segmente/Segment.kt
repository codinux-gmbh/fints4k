package net.codinux.banking.fints.messages.segmente

import net.codinux.banking.fints.messages.Nachrichtenteil
import net.codinux.banking.fints.messages.Separators
import net.codinux.banking.fints.messages.datenelemente.DatenelementBase
import net.codinux.banking.fints.messages.datenelemente.implementierte.DoNotPrintDatenelement
import net.codinux.banking.fints.messages.datenelementgruppen.implementierte.Segmentkopf


abstract class Segment(val dataElementsAndGroups: List<DatenelementBase>) : Nachrichtenteil() {

    companion object {
        val ReplaceEmptyDataElementGroupSeparatorsAtEndPattern = Regex("\\${Separators.DataElementGroupsSeparator}*\$")
    }


    override fun format(): String {
        val formattedSegment = dataElementsAndGroups.filter { it is DoNotPrintDatenelement == false }.joinToString(Separators.DataElementGroupsSeparator) { it.format() }

        return cutEmptyDataElementGroupsAtEndOfSegment(formattedSegment)
    }

    /**
     * Auslassen von Datenelementen durch Abschneiden
     * Ist für DE, die am Ende eines Segments stehen, kein Inhalt vorhanden, können sie ausgelassen werden.
     * In diesem Fall wird das Segmentende-Zeichen unmittelbar nach dem letzten mit Inhalt belegten DE angegeben.
     */
    protected open fun cutEmptyDataElementGroupsAtEndOfSegment(formattedSegment: String): String {
        return ReplaceEmptyDataElementGroupSeparatorsAtEndPattern.replaceFirst(formattedSegment, "")
    }

    override fun toString() = "${dataElementsAndGroups.firstOrNull { it is Segmentkopf }}"

}