import Foundation
import CoreData
import UIKit
import BankingUiSwift


class CoreDataBankingPersistence: IBankingPersistence, ITransactionPartySearcher {
    
    private let mapper = Mapper()
    
    private let context: NSManagedObjectContext
    
    
    init(context: NSManagedObjectContext) {
        self.context = context
        
        // to fix merging / updating cached objects, see Mapper
        context.mergePolicy = NSMergePolicy(merge: NSMergePolicyType.mergeByPropertyObjectTrumpMergePolicyType)
    }
    
    
    func saveOrUpdateBank(bank: IBankData, allBanks: [IBankData]) {
        do {
            let mapped = mapper.map(bank, context)
            
            if bank.technicalId.isCoreDataId == false { // an unpersisted bank (but check should not be necessary)
                context.insert(mapped)
            }
            
            try context.save()
            
            setIds(bank, mapped)
        } catch {
            NSLog("Could not save bank \(bank): \(error)")
        }
    }
    
    private func setIds(_ bank: IBankData, _ mappedBank: PersistedBankData) {
        bank.technicalId = mappedBank.objectIDAsString
        
        for account in bank.accounts {
            if let mappedAccount = mappedBank.accounts?.first { ($0 as! PersistedBankAccount).identifier == account.identifier} as? PersistedBankAccount {
                account.technicalId = mappedAccount.objectIDAsString
            }
        }
        
        for tanMethod in bank.supportedTanMethods {
            if let mappedTanMethod = mappedBank.supportedTanMethods?.first { ($0 as! PersistedTanMethod).bankInternalMethodCode == tanMethod.bankInternalMethodCode } as? PersistedTanMethod {
                tanMethod.technicalId = mappedTanMethod.objectIDAsString
            }
        }
        
        for tanMedium in bank.tanMedia {
            if let mappedTanMedium = mappedBank.tanMedia?.first { ($0 as! PersistedTanMedium).displayName == tanMedium.displayName } as? PersistedTanMedium {
                tanMedium.technicalId = mappedTanMedium.objectIDAsString
            }
        }
    }
    
    
    func readPersistedBanks_() -> [IBankData] {
        var banks: [PersistedBankData] = []
        
        do {
            let request: NSFetchRequest<PersistedBankData> = PersistedBankData.fetchRequest()
            request.returnsObjectsAsFaults = false
            
            try banks = context.fetch(request)
        } catch {
            NSLog("Could not request banks: \(error)")
        }
        
        return banks.map( { mapper.map($0) } )
    }
    
    func deleteBank(bank: IBankData, allBanks: [IBankData]) {
        do {
            let mapped = mapper.map(bank, context)
            
            context.delete(mapped)
            
            try context.save()
        } catch {
            NSLog("Could not delete Bank \(bank): \(error)")
        }
    }
    
    func saveOrUpdateAccountTransactions(account: IBankAccount, transactions: [IAccountTransaction]) {
        if let persistedAccount = context.objectByID(account.technicalId) as? PersistedBankAccount {
            for transaction in transactions {
                if transaction.technicalId.isCoreDataId == false { // TODO: or also update already persisted transactions?
                    do {
                        let mappedTransaction = mapper.map(persistedAccount, transaction, context)
                        
                        try context.save()
                        
                        transaction.technicalId = mappedTransaction.objectIDAsString
                    } catch {
                        NSLog("Could not save transaction \(transaction.buildTransactionIdentifier()) of account \(account.displayName): \(error)")
                    }
                }
            }
        }
    }
    
    
    func saveOrUpdateAppSettings(appSettings: AppSettings) {
        do {
            let mapped = mapper.map(appSettings, context)
            
            if appSettings.technicalId.isCoreDataId == false { // an unpersisted bank (but check should not be necessary)
                context.insert(mapped)
            }
            
            try context.save()
            
            appSettings.technicalId = mapped.objectIDAsString
            if let flickerCodeSettingsId = mapped.flickerCodeSettings?.objectIDAsString {
                appSettings.flickerCodeSettings?.technicalId = flickerCodeSettingsId
            }
            if let qrCodeSettingsId = mapped.qrCodeSettings?.objectIDAsString {
                appSettings.qrCodeSettings?.technicalId = qrCodeSettingsId
            }
            if let photoTanSettingsId = mapped.photoTanSettings?.objectIDAsString {
                appSettings.photoTanSettings?.technicalId = photoTanSettingsId
            }
        } catch {
            NSLog("Could not save AppSettings \(appSettings): \(error)")
        }
    }
    
    func readPersistedAppSettings() -> AppSettings? {
        do {
            let request: NSFetchRequest<PersistedAppSettings> = PersistedAppSettings.fetchRequest()
            request.returnsObjectsAsFaults = false
            
            let persistedSettings = try context.fetch(request)
            
            if let settings = persistedSettings.first {
                return mapper.map(settings)
            }
        } catch {
            NSLog("Could not request AppSettings: \(error)")
        }
        
        return nil
    }
    
    
    func saveUrlToFile(url: String, file: URL) {
        if let remoteUrl = URL.encoded(url) {
            if let fileData = try? Data(contentsOf: remoteUrl) {
                do {
                    try UIImage(data: fileData)?.pngData()?.write(to: file)
                } catch {
                    NSLog("Could not save url '\(url)' to file '\(file): \(error)")
                }
                
                // not indented for this kind of data but at least it works
                UserDefaults.standard.set(fileData, forKey: file.absoluteString)
            }
        }
    }
    
    func readContentOfFile(_ filePath: String) -> Data? {
        return UserDefaults.standard.data(forKey: filePath)
    }
    
    func findTransactionParty(query: String) -> [TransactionParty] {
        var transactions: [PersistedAccountTransaction] = []
        
        do {
            let request: NSFetchRequest<PersistedAccountTransaction> = PersistedAccountTransaction.fetchRequest()
            request.returnsObjectsAsFaults = false
            
            request.predicate = NSPredicate(format: "otherPartyName CONTAINS[c] %@", query)
            
            request.propertiesToFetch = [ "otherPartyName", "otherPartyBankCode", "otherPartyAccountId" ]
            request.sortDescriptors = [ NSSortDescriptor(key: "otherPartyName", ascending: true) ]
            
            try transactions = context.fetch(request)
        } catch {
            NSLog("Could not request banks: \(error)")
        }
        
        let transactionParties = transactions
            .filter { $0.otherPartyAccountId != nil } // if IBAN is not set we cannot make use of it
            .map( { TransactionParty(name: $0.otherPartyName ?? "", iban: $0.otherPartyAccountId, bic: $0.otherPartyBankCode, bankName: nil) } )
        
        let uniqueTransactionParties = Set<TransactionParty>(transactionParties)
        
        return Array(uniqueTransactionParties)
    }
    
    
    func deleteAll() {
        do {
            let request = NSFetchRequest<NSFetchRequestResult>(entityName: "PersistedBankData")
            
            let deleteAll = NSBatchDeleteRequest(fetchRequest: request)
            
            try context.execute(deleteAll)
        } catch {
            NSLog("Could not delete all banks: \(error)")
        }
    }
    
}
