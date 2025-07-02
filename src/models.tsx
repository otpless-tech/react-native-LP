export type CctSupportType = 'Twa' | 'Cct';

export interface CctSupportConfig {
  type: CctSupportType;
  origin: string | null;
}
