import type {FeatureFlag} from './../../../service/ee/feature-flag';

/**
 * Generated from ai.giskard.web.dto.config.LicenseDTO
 */
export interface LicenseDTO {
    id: string;
    active: boolean;
    expiresOn: any /* TODO: Missing translation of java.time.Instant */
    ;
    features: { [key in FeatureFlag]: boolean };
    licenseProblem: string;
    planCode: string;
    planName: string;
    projectLimit: number;
    userLimit: number;
}