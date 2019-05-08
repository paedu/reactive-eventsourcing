// "verkehrsmittel" type (from backend)
export interface Verkehrsmittel {
  vmNummer: number;
  vmArt: string;
  bezeichnung: string;
  fahrtpunkte: string[];
  aktuellePosition: string;
  delay: number;
  arrived?: boolean;
}
