require 'spec_helper'
require 'pry'

feature 'Manage delegations', type: :feature do


  context 'an admin user and a bunch of other users and one delegation' do

    before :each do
      @admins = 3.times.map do
        FactoryBot.create :admin
      end.to_set

      @admin = @admins.first

      @users = 15.times.map do
        FactoryBot.create :user
      end.to_set

      @delegation = FactoryBot.create :delegation

      sign_in_as @admin
    end


    scenario "delete a delegation" do
      visit '/admin/'
      click_on 'Delegations'
      click_on @delegation.firstname
      click_on 'Delete'
      wait_until do
        page.has_content? "Delete Delegation #{@delegation.firstname}"
      end
      click_on 'Delete'
      wait_until do
        page.has_content? "No (more) delegations found."
      end

    end

  end

end
